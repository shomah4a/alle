# ADR 0105: mark-whole-buffer コマンドの追加

## ステータス

承認済み

## コンテキスト

バッファ全体を選択する操作は、全文コピーや全文置換の起点として頻繁に使用される。
Emacsでは `C-x h` (`mark-whole-buffer`) としてこの機能が提供されている。
現在このエディタには同等のコマンドが存在しない。

## 決定

`MarkWholeBufferCommand` を新規作成し、Emacsの `mark-whole-buffer` と同等の動作を実装する。

- pointをバッファ先頭(0)に移動する
- markをバッファ末尾(buffer.length())に設定する
- キーバインドは `C-x h` とする

pointを先頭に置く理由は、Emacsの挙動と一致させるため。
`getRegionStart()` / `getRegionEnd()` は `min`/`max` で算出されるため、
mark/pointの位置関係にかかわらずリージョン範囲は同一だが、
カーソル表示位置がバッファ先頭になることがユーザーの期待と合致する。

## 影響

- 新規ファイル: `MarkWholeBufferCommand.java`
- `EditorCore.java` にコマンド登録とキーバインド追加（各1行）
