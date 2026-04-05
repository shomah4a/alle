# ADR-0107: ユーザーモジュールパスの導入とユーザー初期化の責務移動

## ステータス

採用

## コンテキスト

`~/.alle.d/init.py` によるユーザー初期化スクリプト機構は存在するが、設定が大きくなるとファイル分割の需要が生じる。
Python であれば `~/.alle.d/modules` を `sys.path` に追加しておけば、`init.py` から `import` で読み込める。

また、ユーザー初期化スクリプトの読み込みロジック (`loadUserInit`) が `alle-app` の `Main.java` にあり、スクリプトエンジンの初期化方法を `alle-app` が知っている状態になっていた。
この責務は `alle-script` 側にあるべきである。

## 決定

### `~/.alle.d/modules` を `sys.path` に追加する

`GraalPyEngineFactory.create()` 内で `sys.path.append()` により modules ディレクトリを追加する。
`sys.path.insert(0, ...)` ではなく `append` を使用し、組み込み `alle` モジュールの優先度を維持する。
ディレクトリが存在しなくても追加する（Python の仕様上、存在しないパスは import 時にスキップされる）。

### ユーザー初期化の読み込みを `ScriptEngine` のメソッドとして提供する

`ScriptEngine` インターフェースに `loadUserInit(Path userConfigDir)` メソッドを追加し、`ScriptResult` を返す。
各言語エンジンが初期化ファイル名（Python なら `init.py`）を決定する。

`Main.java` は結果の `ScriptResult` を受け取り、`MessageBuffer` への出力のみ行う。

### `alleDotD` パスはファクトリで受け取る

`~/.alle.d` のパスは `GraalPyEngineFactory` のコンストラクタ引数として受け取る。
将来コマンドライン引数や環境変数で変更可能にするための設計。

## 影響

- `ScriptEngine` インターフェースにメソッド追加
- `GraalPyEngine` に `loadUserInit` 実装を追加
- `GraalPyEngineFactory` コンストラクタに `alleDotD` パス引数追加
- `Main.java` の `loadUserInit` static メソッドを削除し、`ScriptEngine.loadUserInit()` 呼び出しに置換
