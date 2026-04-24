# ADR 0128: FileOpenService の導入と起動時ファイルオープン

## ステータス

提案

## コンテキスト

現在、ファイルを開くロジックは `FindFileCommand.openFile` の private メソッドに閉じている。
起動時にコマンドライン引数でファイルパスを受け取って開く機能を追加するにあたり、
同じロジックを再利用する必要がある。

また、今後 find-file 以外のコンテキスト（スクリプトエンジン経由等）から
ファイルを開きたいケースも想定される。

## 決定

### FileOpenService の導入

`FindFileCommand.openFile` のファイルオープンロジックをサービス層 `FileOpenService` に切り出す。

- パッケージ: `io.github.shomah4a.alle.core.io`（BufferIO と同じ IO 関連パッケージ）
- 責務: パス正規化、既存バッファ検索、ファイル読み込み（不在時は空バッファ作成）、メジャーモード設定、バッファ登録、ウィンドウ切り替え
- ディレクトリ判定 + TreeDired 委譲は FindFileCommand 側に残す（サービスはファイルのみ担当）

### FindFileCommand の簡素化

コンストラクタから `bufferIO`, `autoModeMap`, `modeRegistry` を除去し、`FileOpenService` を受け取る。
FindFileCommand の責務は「ユーザー入力 → パス取得 → ディレクトリ/ファイル振り分け」に限定される。

### 起動時ファイルオープン

Main.java の `run()` メソッドで、ユーザー初期化スクリプト読み込み後・`EditorRunner.run()` 前に
コマンドライン引数のファイルパスを `FileOpenService.openFile` で開く。

スクリプトエンジン初期化後に配置することで、ユーザーカスタマイズ（モードフック等）が反映された状態でファイルが開かれる。

## 結果

- ファイルオープンロジックが再利用可能になる
- FindFileCommand のコンストラクタ引数が 7 から 5 に減り、責務が明確になる
- 起動時にファイルパスを指定してエディタを開始できる
