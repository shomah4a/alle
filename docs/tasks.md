# タスクバックログ

## 未着手タスク

### M-x (execute-extended-command)
- ミニバッファでコマンド名を入力して実行する
- CommandNameCompleter でコマンド名のTab補完
- M-x にバインド
- ExecuteCommandCommand と executeByName() が既に存在するので拡張する形
- CommandRegistry.registeredNames() も既に存在

## 将来課題

### CommandContext のスナップショット化
- コマンド呼び出し時の activeWindow 等をスナップショットとして保存
- 現在は WindowActor が部分的にこの役割を担っている
- 非同期コマンドが増えた場合の安全性向上

### Tab補完の候補リスト表示
- Tab を複数回押した場合に候補リストバッファを開く
- Emacs の *Completions* バッファ相当

### M-x 経由の lastCommand 更新
- M-x で実行されたコマンドの名前を lastCommand に設定すべき
- Emacs では execute-extended-command ではなく実際のコマンドが last-command になる
- 現時点では lastCommand 依存機能がないため後回し可

### MinorMode の同一性判定
- enableMinorMode の重複チェックが equals/hashCode 未定義で曖昧
- name() ベースの判定か equals/hashCode 定義かを決める
- 具象実装追加時に対応

### CommandContext のビルダーパターン
- フィールド数が多く（9個）、テストコードでの生成が煩雑
- TestCommandContextFactory が部分的にカバーしているが、本体側にもビルダーが欲しい
- テストの可読性向上のため

### MajorMode/MinorMode の共通インターフェース
- 同一メソッドシグネチャ (name(), keymap()) を持つが共通親がない
- 意味論的に異なる概念なので現時点では不要
- 共通化が必要になった時点で検討
