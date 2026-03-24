# スレッドセーフ性調査レポート

- 調査日: 2026-03-24
- 調査対象: Actorモデル外側でスレッドセーフ性が毀損されうる箇所
- 確度: 0.95

## 背景

ADR-0058 でActor（BufferActor, WindowActor, FrameActor）を唯一の公開操作APIとする方針を決定した。
将来的にActor内部にキュー+専用スレッドを導入してマルチスレッド化する計画がある。
Actor内部は `atomicPerform` で排他制御を行う設計だが、Actor外側にミュータブルな共有状態が残存している場合、スレッドセーフ性が保証されない。

## CRITICAL

### MessageBuffer の showingMessage フラグと lines

- **ファイル**: `alle-core/src/main/java/.../buffer/MessageBuffer.java`
- **箇所**: `showingMessage`(27行目), `lines`(24行目)
- **問題**:
  - `showingMessage` boolean フラグがEditorThread（`message()`, `clearShowingMessage()`）とRenderThread（`createSnapshot()` → `isShowingMessage()`）の間で無保護に読み書きされる
  - `lines`（RingBuffer）も同様に、`message()`での書き込みとスナップショット生成時の読み取りが競合する
- **マルチスレッド化時**: race conditionによるダーティリード、ConcurrentModificationException
- **修正方向**: synchronizedメソッド化、またはAtomicBoolean + concurrent RingBuffer

## HIGH

### KillRing の index と entries

- **ファイル**: `alle-core/src/main/java/.../command/KillRing.java`
- **箇所**: `index`(17行目), `entries`(15行目)
- **問題**: `push()`, `appendToLast()`, `current()` が非同期コマンド実行から呼ばれうるが同期化されていない
- **マルチスレッド化時**: 連続kill/yankで`index`が不正な状態になる
- **修正方向**: synchronized保護

### CommandRegistry の commands

- **ファイル**: `alle-core/src/main/java/.../command/CommandRegistry.java`
- **箇所**: `commands` MutableMap(14行目)
- **問題**: スクリプト側からの`registerOrReplace()`と`lookup()`が同時実行されうる
- **マルチスレッド化時**: ConcurrentModificationExceptionまたはNPE
- **修正方向**: ConcurrentHashMapまたはsynchronized

### BufferManager の currentIndex

- **ファイル**: `alle-core/src/main/java/.../buffer/BufferManager.java`
- **箇所**: `currentIndex`(18行目), `buffers`(16行目), `actorMap`(17行目)
- **問題**: `add()`, `remove()`, `switchTo()` 内で`currentIndex`の読み書きが複数操作にまたがる
- **マルチスレッド化時**: バッファ追加/削除中のcurrentIndex不整合
- **修正方向**: synchronizedメソッド化

### EditableBuffer / Window への直接アクセス

- **ファイル**: `alle-core/src/main/java/.../buffer/EditableBuffer.java`(24-32行目), `alle-core/src/main/java/.../window/Window.java`(17-22行目)
- **問題**: `BufferActor.getBuffer()`, `WindowActor.getWindow()` エスケープハッチにより、Actorの排他制御を迂回して直接フィールドにアクセスできる
- **マルチスレッド化時**: inconsistent snapshot、フィールドの中間状態読み取り
- **修正方向**: ADR-0058方針に従いエスケープハッチの段階的廃止

## MEDIUM

### Keymap の静的フィールド

- **ファイル**: `alle-core/src/main/java/.../keybind/Keymap.java`
- **箇所**: `quitCommand`(17行目), `defaultCommand`(21行目)
- **問題**: 静的フィールドおよびインスタンスフィールドが非同期アクセスで保護されていない
- **修正方向**: AtomicReference

### CommandLoop.resolveKey() の .join()

- **ファイル**: `alle-core/src/main/java/.../command/CommandLoop.java`
- **箇所**: 133行目
- **問題**: `frameActor.resolveKey().join()` で同期ブロック。将来キュー化した際にデッドロック要因
- **修正方向**: 非同期チェーン化

### EditorThread.publishSnapshot() の .join()

- **ファイル**: `alle-tui/src/main/java/.../tui/EditorThread.java`
- **箇所**: 64行目
- **問題**: `createSnapshot().thenAccept().join()` で同期ブロック
- **修正方向**: 非同期チェーン化（ただしEditorThreadのループ構造上、何らかの待機は必要）

### Frame.minibufferActive

- **ファイル**: `alle-core/src/main/java/.../window/Frame.java`
- **箇所**: 22行目
- **問題**: `FrameActor.getFrame()` エスケープハッチで保護外になりうる
- **修正方向**: エスケープハッチの廃止

### HistoryNavigator.cursor

- **ファイル**: `alle-core/src/main/java/.../input/HistoryNavigator.java`
- **箇所**: 18行目
- **問題**: 複数プロンプトセッションが同時進行した場合にcursor競合
- **現状**: セッションごとに新規生成されるため当面問題なし

## LOW

### MinibufferPreviousHistoryCommand.firstNavigation

- **ファイル**: `alle-tui/src/main/java/.../tui/MinibufferInputPrompter.java`
- **箇所**: 274行目
- **問題**: boolean フィールドを持つが、毎回新規インスタンスが作成されるため当面問題なし
- **潜在リスク**: コマンドレジストリに登録されて使い回される設計に変更された場合に顕在化

## 所見

Actor内部の排他制御（atomicPerform）は正しく設計されているが、Actor外側のミュータブル共有状態がスレッドセーフ性のボトルネックになる。特にMessageBuffer、KillRing、CommandRegistry、BufferManagerはActorとは独立した共有リソースであり、マルチスレッド化の前に個別の同期化対策が必要。

また、`getBuffer()`/`getWindow()`/`getFrame()` エスケープハッチはADR-0058で廃止方針が示されているが、まだ残存しておりスナップショット生成等で使われている。これらの完全除去がスレッドセーフ化の前提条件となる。
