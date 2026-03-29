# ADR 0079: コマンドデリゲート機構

## ステータス

承認

## コンテキスト

コマンドの実装内部から別のコマンドをプログラム的に呼び出したいケースがある。
現状では `ExecuteCommandCommand.executeByName` が M-x 経由でコマンドを名前実行する機能を持っているが、
任意のコマンドから汎用的に別コマンドを呼び出す手段がない。

## 決定

`CommandContext` に `CommandRegistry` への参照を持たせ、`delegate(String commandName)` メソッドを追加する。

### delegate の挙動

- `CommandRegistry` から名前でコマンドを検索し、`command.execute(this)` を呼び出す
- context はそのまま渡す（M-x から呼んだときと同様）
  - `thisCommand` は元のコマンド名のまま
  - `lastCommand` も元のまま（CommandLoop での更新は元のコマンド名で行われる）
  - `triggeringKey` も元のまま
- 戻り値は `CompletableFuture<Void>`
- 存在しないコマンド名の場合は `IllegalArgumentException`
- delegate 先のコマンドは呼び出し元と同じ undo トランザクション内で実行される

### スクリプト側への公開

- `ScriptCommandContext` に `delegate(String commandName)` を追加
- Python 側の `CommandContext` にも `delegate(command_name)` を追加

## 影響

- `CommandContext` record にフィールドが追加されるため、全生成箇所の修正が必要
- `CommandLoop` のコンストラクタに `CommandRegistry` パラメータが追加される
- `ExecuteCommandCommand.executeByName` と同様のロジックが `CommandContext.delegate` にも存在するが、
  用途が異なる（UI操作 vs プログラム的呼び出し）ため統合は行わない
