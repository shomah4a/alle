# ADR-0053: save-buffer保存成功時のミニバッファメッセージ表示

## ステータス

承認

## コンテキスト

save-bufferコマンドでバッファをファイルに保存した際、保存が成功したかどうかのフィードバックがユーザーに表示されない。
Emacsではsave-buffer実行後にエコーエリアに「Wrote /path/to/file」と表示される。
同様のフィードバックを提供することで、保存操作の成否をユーザーが確認できるようにする。

## 決定

`SaveBufferCommand.saveBuffer()` で `bufferIO.save(buffer)` 成功後に、`context.messageBuffer().message("Saved: " + filePath)` でエコーエリアにメッセージを表示する。

`saveBuffer()` が呼ばれる時点ではファイルパスは必ず設定済み（設定済み分岐またはプロンプト入力後に呼ばれる）であるため、`getFilePath().orElseThrow()` で取得する。

## 影響

- SaveBufferCommandのsaveBufferメソッドに1行追加
- 既存の動作には影響なし（メッセージ表示が追加されるのみ）
