# ADR 0088: モードのコマンド名前空間化

## ステータス

承認

## コンテキスト

現在、コマンドはグローバルなCommandRegistryに一元管理されている。
モードはキーバインドやスタイリングを提供するが、コマンドの所有権を持たない。
そのため、モード固有のコマンド（例: TreeDiredの操作コマンド群）もグローバル空間に登録されており、名前の肥大化やスコープの曖昧さが生じている。

モードをコマンドの名前空間として再定義し、モード固有のコマンドをモードに帰属させる。

## 決定

### コマンド名の制約

コマンド名に使用可能な文字は `[A-Za-z0-9-]` に限定する。
`.` はFQCN（完全修飾コマンド名）の区切り文字として予約し、コマンド名自体には使用できない。

### モードのCommandRegistry

MajorMode/MinorModeインターフェースに `commandRegistry()` メソッドを追加する。
デフォルト実装は `Optional.empty()` を返し、既存のモード実装に影響を与えない。

### コマンド名前解決（CommandResolver）

コマンドの名前解決は以下の優先順位で行う。

1. **FQCN**: `mode-name.command-name` 形式で指定された場合、該当モードのレジストリから直接解決する。`global.command-name` はグローバルレジストリから解決する
2. **MinorMode**: バッファで有効なマイナーモードのレジストリを後勝ちで検索する
3. **MajorMode**: バッファのメジャーモードのレジストリを検索する
4. **グローバル**: グローバルCommandRegistryを検索する

この優先順位はキーマップの解決順序（CommandLoop.resolveKey）と一致する。

### delegate()のセマンティクス

`CommandContext.delegate(commandName)` はCommandResolverの階層解決を使用する。
グローバルコマンドを明示的に呼び出したい場合は `delegate("global.command-name")` とする。
通常の `delegate("command-name")` は現在のバッファのモードスコープを考慮した解決を行う。

### M-x補完

ExecuteCommandCommand（M-x）の補完候補は以下のルールで生成する。

- バッファのモードに属するコマンドは短い名前（コマンド名のみ）で候補に出す
- グローバルコマンドは短い名前で候補に出す
- 他モードのコマンドをFQCN形式で候補に出す機能は将来対応とする

### TreeDiredの移行方針

- `tree-dired`（TreeDiredCommand）はdiredバッファを開くコマンドであり、グローバルに残す
- 残りの11コマンド（toggle, find-file-or-toggle, up-directory, refresh, mark, unmark, toggle-mark, copy, rename, delete, chmod, chown）はTreeDiredModeのCommandRegistryに移行する
- グローバルレジストリからは削除する

### 将来の拡張: コマンドのインポート

モードのCommandRegistryに他のCommandRegistryの内容をインポートする機構は将来の拡張として残す。
現時点ではモードとコマンド群は1:1の関係とする。
複数モードでのコマンド共有が必要になった時点で `importFrom` 等のAPIを追加する。

## 影響

### 基盤（本ADRで実装）

- MajorMode.java / MinorMode.java: commandRegistry() メソッド追加
- CommandRegistry.java: コマンド名バリデーション追加
- CommandResolver.java: 新設（階層的名前解決）
- CommandContext.java: commandRegistryフィールドをCommandResolverに置換
- CommandLoop.java: CommandContext生成の変更
- ExecuteCommandCommand.java: CommandResolver経由に変更
- CommandNameCompleter.java: CommandResolver経由に変更
- EditorCore.java: CommandResolver生成・注入

### 別タスクで実施

- TreeDiredMode.java: CommandRegistry保持、TreeDiredコマンドのモードスコープ移行
- CommandResolver.allCommandNames: 他モードのFQCN形式候補の対応
