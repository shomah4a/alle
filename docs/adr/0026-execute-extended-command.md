# ADR 0026: M-x (execute-extended-command) の実装

## ステータス

承認

## コンテキスト

コマンドをキーバインド経由でしか実行できない。
Emacs の M-x に相当する、コマンド名を入力して実行する機能を実装する。

## 決定

### 基本動作
- M-x でミニバッファにプロンプト表示
- コマンド名を入力（CommandNameCompleter によるTab補完付き）
- 確定後に CommandRegistry からコマンドを検索して実行

### 実装方針
- 既存の ExecuteCommandCommand.execute() を拡張する
- CommandNameCompleter を新規作成（CommandRegistry.registeredNames() を利用）
- キーバインドは M-x (Meta+x)

### 制約事項
- M-x 経由で実行されたコマンドの lastCommand 更新は将来課題とする
  - Emacs では実際のコマンド名が last-command になるが、現時点では依存機能がない

## 影響

- ExecuteCommandCommand.execute() が実装される
- CommandNameCompleter が新規作成される
- Main.java に M-x キーバインドが追加される
