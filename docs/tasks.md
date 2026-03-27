# タスクバックログ

## 未着手タスク

### スクリプトエンジン: BufferFacade のスレッド安全性
- BufferFacade の insertAt/deleteAt が Buffer を直接操作しており WindowActor を経由していない
- WindowActor が非同期化された場合にレースコンディションの温床になる
- BufferFacade を読み取り専用にするか、WindowActor 経由に統一する

### スクリプトエンジン: 初期化の遅延実行
- スクリプトエンジンの初期化を EditorRunner.run() 開始後に遅延する
- 現在は描画スレッド起動前に初期化しているため、初期化中のメッセージが画面に表示されない
- エンジンスレッドの最初のタスクとして初期化を行う仕組みが必要

### スクリプトエンジン: init-loader
- `.alle.d/init-loader` からスクリプトファイルを自動読み込みする仕組み
- alle-script モジュール内にローダーを実装

### スクリプトエンジン: 未完了 Future の asyncio 統合
- 現在の JavaFuture.__await__ は完了済み Future のみ対応
- asyncio イベントループとの統合で未完了 Future を非同期待ちできるようにする

### Pythonモード: 自動dedent
- `return`/`pass`/`break`/`continue`/`raise` の入力完了後に自動でインデントを減らす
- newline-and-indent では対応済みだが、入力中のリアルタイム dedent は未実装

## 将来課題

### CommandContext のスナップショット化
- コマンド呼び出し時の activeWindow 等をスナップショットとして保存
- 現在は WindowActor が部分的にこの役割を担っている
- 非同期コマンドが増えた場合の安全性向上

### M-x 経由の lastCommand 更新
- M-x で実行されたコマンドの名前を lastCommand に設定すべき
- Emacs では execute-extended-command ではなく実際のコマンドが last-command になる
- 現時点では lastCommand 依存機能がないため後回し可

### CommandContext のビルダーパターン
- フィールド数が多く（9個）、テストコードでの生成が煩雑
- TestCommandContextFactory が部分的にカバーしているが、本体側にもビルダーが欲しい
- テストの可読性向上のため

### MajorMode/MinorMode の共通インターフェース
- 同一メソッドシグネチャ (name(), keymap()) を持つが共通親がない
- 意味論的に異なる概念なので現時点では不要
- 共通化が必要になった時点で検討

