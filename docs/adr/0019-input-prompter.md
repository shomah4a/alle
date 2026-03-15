# ADR-0019: 汎用入力待機機能（InputPrompter）

## ステータス

承認

## コンテキスト

find-file、save-buffer、M-x等の対話的コマンドを実装するにあたり、
ユーザーから文字列入力を受け付ける汎用的な仕組みが必要になった。

Emacsのread-from-minibufferに相当するが、TUI/GUIを問わず利用できる
抽象インターフェースとして設計する。

## 決定

### 1. PromptResult sealed interfaceで入力結果を表現する

確定（Confirmed）とキャンセル（Cancelled）を型で区別する。
例外ではなくパターンマッチで分岐できるようにする。

### 2. InputPrompterインターフェースをalle-coreに定義する

`CompletableFuture<PromptResult> prompt(String message)` を持つ。
TUIではミニバッファ、将来のGUIではダイアログで実装できる。

### 3. プロンプトはCommandLoopをブロックしない

promptはCompletableFutureを返して即座にreturnする。
コマンドの後続処理はthenAccept/thenComposeで繋ぐ。
プロンプト入力中もC-oによるウィンドウ切替等の操作が可能。

### 4. バッファがキーマップを持つ

バッファにOptional<Keymap>を追加し、キー解決時に
アクティブバッファのキーマップ → グローバルキーマップの順で探索する。
ミニバッファのバッファに専用キーマップを持たせることで、
RET→確定、C-g→キャンセル等の操作を実現する。

### 5. ミニバッファは入力受付中のみフォーカス切替可能

Frame.setActiveWindowの制約を緩和し、ミニバッファが入力受付中の場合に
限りフォーカス対象とする。ミニバッファはWindowTreeには含めない。

### 6. CommandLoopのコマンド実行をfire-and-forget化する

コマンドが返すCompletableFutureをjoinせず、
lastCommandの更新等はthenRunで繋ぐ。
これによりプロンプト入力中もメインループが動き続ける。

## 却下した選択肢

### Virtual Thread + joinによるブロックモデル

プロンプト入力中にメインループがブロックされるため、
ウィンドウ切替等の操作ができない。

### ミニバッファをWindowTreeに含める

分割・削除の対象にならないよう制約が必要で、
複数行展開時のレイアウト管理もWindowTreeとは別枠の方が自然。

### Mode抽象化の同時導入

スコープが広がりすぎるため今回は見送り。
isearch等の実装時に改めて設計する。

## 影響

- CommandLoopのコマンド実行モデルが同期→非同期に変更
- BufferにOptional<Keymap>が追加される
- Frame.setActiveWindowの制約が緩和される
- CommandContextにInputPrompterフィールドが追加される
