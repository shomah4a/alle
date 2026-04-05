# ADR 0106: スクリプトコマンドへのトリガーキーシーケンス公開

## ステータス

承認済み

## コンテキスト

Java側の `CommandContext` には `Optional<KeyStroke> triggeringKey` としてコマンドを発動したキーストロークが保持されている。
`SelfInsertCommand` や `ISearchSelfInsertCommand` 等のJava実装コマンドはこの情報を利用できるが、スクリプト（Python）側の `ScriptCommandContext` にはアクセス手段が提供されていない。

また、現状の `triggeringKey` は単一の `KeyStroke` しか保持しないため、`C-x C-f` のようなプレフィックスキーシーケンスの場合、最後のキーストロークしか取得できない。

## 決定

### CommandContext の変更

`Optional<KeyStroke> triggeringKey` を `ListIterable<KeyStroke> triggeringKeySequence` に変更する。

- プレフィックスキーを含むキーシーケンス全体を保持する
- プログラム的呼び出し時は空リスト
- 単一キー（例: `a`）は要素1つのリスト
- プレフィックスキー（例: `C-x C-f`）は要素2つのリスト

### CommandLoop の変更

`PendingPrefix` にプレフィックスキーストロークのリストを蓄積し、コマンド実行時に最終キーと連結してシーケンス全体を渡す。

### 既存コマンドの移行

`SelfInsertCommand` や `ISearchSelfInsertCommand` など、`triggeringKey()` を使用していた箇所はシーケンスの末尾要素を参照するように変更する。

### スクリプト側の公開

`ScriptCommandContext` に `triggeringKeySequence()` メソッドを追加し、Python側に `KeyStroke` ラッパークラスと `CommandContext.triggering_key_sequence()` メソッドを提供する。

Python側の `KeyStroke` ラッパーは以下のAPIを持つ:

- `key_code: int` - キーコード
- `modifiers: frozenset[str]` - 修飾キーの集合
- `display_string() -> str` - 表示文字列
- `has_ctrl() -> bool` - Ctrl修飾の有無
- `has_meta() -> bool` - Meta修飾の有無
- `has_shift() -> bool` - Shift修飾の有無

## 結果

- スクリプトから登録したコマンドでトリガーされたキーシーケンス全体を取得可能になる
- `self-insert-command` 等の既存コマンドの動作は変わらない
