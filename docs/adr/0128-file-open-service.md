# ADR 0128: PathOpenService / DiredOpenService の導入と起動時ファイルオープン

## ステータス

採用

## コンテキスト

ファイルを開くロジックは `FindFileCommand.openFile` の private メソッドに閉じていた。
起動時にコマンドライン引数でファイルパスを受け取って開く機能を追加するにあたり、
同じロジックを再利用する必要がある。

また、`TreeDiredCommand.openDired` にもディレクトリを開くロジックが閉じていた。
`TreeDiredFindFileOrToggleCommand` にもファイルオープンのロジックが重複していた。

## 決定

### PathOpenService の導入

パスを開くサービス `PathOpenService` を `io.github.shomah4a.alle.core.io` パッケージに導入する。

- ファイルの場合: パス正規化、既存バッファ検索、ファイル読み込み（不在時は空バッファ作成）、メジャーモード設定、バッファ登録、ウィンドウ切り替え
- ディレクトリの場合: `DirectoryOpener` インターフェース経由で `DiredOpenService` に委譲

### DiredOpenService の導入

`TreeDiredCommand.openDired` のロジックを `DiredOpenService` として dired パッケージに切り出す。
`TreeDiredCommand` は `DiredOpenService` に委譲する。

### コマンドの簡素化

- `FindFileCommand`: `PathOpenService` に委譲するだけに簡素化。ディレクトリ判定も不要に。
- `TreeDiredCommand`: `DiredOpenService` に委譲するだけに簡素化。
- `TreeDiredFindFileOrToggleCommand`: `CommandContext.openPath()` 経由で `PathOpenService` を利用。コンストラクタ依存ゼロ。

### CommandContext への openPath メソッド追加

`CommandContext` に `PathOpenService` を保持し、`openPath(String)` メソッドを公開する。
コマンドからはサービスの存在を意識せず `context.openPath(path)` で呼べる。

### 起動時ファイルオープン

Main.java の `run()` メソッドで、ユーザー初期化スクリプト読み込み後・`EditorRunner.run()` 前に
コマンドライン引数のファイルパスを `PathOpenService.open` で開く。
ディレクトリが指定された場合も Tree Dired で開かれる。

### 構築順序

循環依存を回避するため以下の順序で構築する:

1. `TreeDiredInitializer.initialize` → `DiredOpenService` + `TreeDiredCommand`
2. `PathOpenService`（`DiredOpenService::openDired` を `DirectoryOpener` として渡す）
3. `FindFileCommand`（`PathOpenService` を渡す）

## 結果

- ファイル/ディレクトリオープンロジックが再利用可能になった
- FindFileCommand、TreeDiredCommand、TreeDiredFindFileOrToggleCommand がそれぞれ簡素化された
- 起動時にファイルパスやディレクトリパスを指定してエディタを開始できる
