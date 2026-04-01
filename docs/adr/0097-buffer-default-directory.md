# ADR 0097: バッファのデフォルトディレクトリ導出

## ステータス

承認

## コンテキスト

`find-file` や `tree-dired` コマンドの起点ディレクトリはプロセスのカレントディレクトリ（cwd）に固定されている。
ファイルを開いているバッファから `find-file` を実行した場合でも、そのファイルのディレクトリではなくcwdが初期値になるため、
Emacsの `default-directory` に相当する動作が実現できていない。

## 決定

`BufferFacade` に `getDefaultDirectory(Path fallback, Predicate<Path> isDirectory)` メソッドを追加する。

- バッファにファイルパスがあり、それがディレクトリであればそのまま返す
- バッファにファイルパスがあり、それがファイルであれば親ディレクトリを返す
- ファイルパスがない場合、または親ディレクトリが取得できない場合は `fallback` を返す
- ディレクトリ判定は `Predicate<Path>` で外部から注入する（副作用の外部化）
- 新しいフィールドは追加しない（既存の `filePath` から導出するのみ）

Emacsの `default-directory` はバッファローカル変数として状態を持つが、
現時点では状態を増やさず導出で済ませる方針とする。
将来的に `cd` コマンド等でバッファ単位のディレクトリを明示的に変更したい場合は、
フィールドの追加を改めて検討する。

## 適用対象

- `FindFileCommand`: ミニバッファの初期値にアクティブバッファの `getDefaultDirectory` を使用
- `TreeDiredCommand`: 同上。加えて、Diredバッファ作成時にディレクトリパスを `filePath` に設定する。`getDefaultDirectory` はディレクトリ判定によりそのパスをそのまま返す

## 適用外

- `SaveBufferCommand`: プロンプトが出るのはファイルパス未設定のバッファのみであり、`getDefaultDirectory` は常にフォールバック値を返すため変更不要
- `ShellCommandExecutor`: スコープ外（別途検討）

## 結果

ファイルを開いているバッファから `find-file` や `tree-dired` を実行すると、
そのファイルのディレクトリが起点になる。
パスのないバッファ（`*scratch*` 等）からの実行は従来通りcwdが起点となる。
