# ADR 0040: コマンド・キーマップ登録のファクトリ化

## ステータス

承認

**注記:** QuitCommand の TUI 固有扱いは [ADR 0062](0062-quit-command-to-core.md) で変更された。

## コンテキスト

デフォルトのコマンド登録・キーバインド設定・メッセージバッファ等のコア初期化が
`alle-app/Main.java` に直書きされている。
これらはエディタのコア機能であり、`alle-core` に属するべきである。

TUI固有のコマンド（`QuitCommand`）は `alle-tui` に存在するため、
全てをcoreに移すのではなく、coreの初期化をファクトリに切り出し、
app層ではTUI固有のコマンドのみ追加登録する。

## 決定

### EditorCore（core）

- コア初期化を一括で行うファクトリクラス
- staticファクトリメソッド `create` で以下を構築する:
  - scratch buffer, minibuffer, Frame
  - MessageBuffer, BufferManager
  - AutoModeMap（デフォルトモード登録含む）
  - CommandRegistry（全coreコマンド登録）
  - Keymap（デフォルトキーバインド構築、`Keymap.setQuitCommand()` 含む）
  - KeyResolver, KillRing, CommandLoop
- 外部依存はcreateメソッドの引数で受け取る:
  - `InputSource`, `Function<Frame, InputPrompter>`, `BufferIO`, `DirectoryLister`, `ShutdownRequestable`
- `InputPrompter` はFrameに依存するため、`Function<Frame, InputPrompter>` として遅延生成する

### Main.java（app）

- `EditorCore.create` を使ってコアを構築する
- TUI固有のコマンド（`QuitCommand`）の登録と `C-q` のバインドのみMain側で追加する
- TUI固有のオブジェクト（`TerminalInputSource`, `ScreenRenderer`, `EditorRunner`）の生成はMain側で行う

## 影響

- デフォルトコマンド・キーバインド・バッファ初期化の定義がcore層に集約される
- app層はTUI固有の拡張のみを担当する
- 将来GUI等の別フロントエンドを追加する際に、コア機能を再利用しやすくなる
