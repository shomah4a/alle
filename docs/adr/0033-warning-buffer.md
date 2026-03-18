# ADR 0033: *Warnings* バッファとエラーハンドリング

## ステータス

承認

## コンテキスト

現状、エディタ内でエラーが発生した場合（コマンド実行時の例外、ファイル保存失敗など）、`java.util.logging.Logger` でログ出力しているのみで、ユーザーには一切通知されない。
エラーの発生をユーザーが認識できる仕組みが必要である。

## 決定

### *Warnings* バッファの導入

- `MessageBuffer` のインスタンスとして `*Warnings*` バッファを作成する
- エラー発生時にメッセージとスタックトレースを行単位で書き込む
- `BufferManager` に登録し、`switch-to-buffer` で閲覧可能にする

### CommandContext.handleError

- `CommandContext` record に `warningBuffer` フィールドを追加
- `handleError(String message, Throwable ex)` メソッドを追加
  - `messageBuffer` にエコーエリア用の短いメッセージを出力
  - `warningBuffer` にメッセージ + スタックトレース全行を書き込む
- コマンド内からのエラー通知を統一的に行える

### エコーエリア連携

- エラー発生時、エコーエリアには短いメッセージを表示（Emacsスタイル）
- 詳細（スタックトレース）は `*Warnings*` バッファで確認する

### warningBuffer の showingMessage について

- `MessageBuffer.message()` は `showingMessage = true` を設定するが、`warningBuffer` に対して `clearShowingMessage()` は呼ばない
- `showingMessage` フラグはエコーエリア表示制御用であり、`*Warnings*` バッファでは使用しない
- ScreenRenderer は `messageBuffer` のみを参照するため実害はない

## 影響

- `CommandContext` record にフィールド追加（コンパイル破壊的変更、生成箇所は限定的）
- `CommandLoop` の `exceptionally` ハンドラを `handleError` 呼び出しに変更
- `SaveBufferCommand` のエラーハンドリングを `handleError` 呼び出しに変更
- `java.util.logging.Logger` によるログ出力はデバッグ用に残す
