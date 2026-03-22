# ADR-0054: switch-to-bufferで存在しないバッファ名の場合に新規バッファを作成

## ステータス

承認

## コンテキスト

現在のswitch-to-bufferコマンドは、入力されたバッファ名がBufferManagerに存在しない場合、何も行わない。
Emacsでは存在しないバッファ名を指定すると、ファイルに紐づかない空のバッファが新規作成されて切り替わる。

## 決定

switch-to-bufferコマンドで存在しないバッファ名が入力された場合、ファイルパスなしのEditableBufferを新規作成し、BufferManagerに登録してウィンドウを切り替える。
作成時にミニバッファへ「Buffer created: バッファ名」のメッセージを表示する。

## 影響

- SwitchBufferCommand.switchBuffer()メソッドに分岐を追加
- 既存のテスト「存在しないバッファ名では何も変わらない」は仕様変更に伴い置き換え
