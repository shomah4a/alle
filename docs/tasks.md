# タスクバックログ

## 未着手タスク

### Dependabot の導入
- CI（ADR 0110）のマージ後に着手する
- minor/patch更新は全依存を1つのPRにまとめる
- major更新はtesting / build-tools / runtime の3カテゴリに分けてPRを作成する
- GitHub Actionsエコシステムも対象に含める

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

### RangeList のオーバーラップ合成対応
- RangeList のオーバーラップ時に値を合成（merge）する仕組みを入れる
  - FaceSpec.merge は実装済み（ADR 0073）
  - `RangeList<V>` の put 時にオーバーラップ部分で合成する仕組みが必要
  - Flag（readOnly, pointGuard）は自明な merge（常に自身を返す）

### テーマのカスタマイズ・永続化
- FaceTheme を差し替え可能にする仕組みは導入済み（ADR 0073）
- ユーザー設定ファイルからのテーマ読み込み、スクリプトからのテーマ拡張登録が未実装
- DefaultFaceTheme に Python モード固有の FaceName マッピングがハードコードされている問題を解消する

### 構文解析: toSyntaxNodeの遅延変換
- rootNode()呼び出しで全構文木がJavaオブジェクトにコピーされる
- 子ノードの遅延変換でパフォーマンスを改善する

### electric_pairコマンドのctx経由化
- 現在はalle.active_window()を直接呼んでいる
- ctx経由でウィンドウ・バッファにアクセスするよう変更する

### kill-buffer 後の切り替え先が previousBuffer を考慮しない
- KillBufferCommand.doKill で切り替え先を決定する際、previousBuffer を参照していない
- findReplacementBuffer が bufferManager から適当なバッファを選んでいる
- さらに setBuffer → clearPreviousBufferIf の順序により previousBuffer が消える
- dired からファイルを開き C-x k で閉じると dired に戻れない問題の原因

### TreeDiredInitializer: 副作用の外部化
- TreeDiredInitializer.initialize() 内で ZoneId.systemDefault() と Path.of("").toAbsolutePath() を直接呼んでいる
- コーディング規約「副作用の外部化」に基づき、引数経由で注入する設計に改修する
- EditorCore からの移動前から存在していた問題であり、リファクタリングで導入されたものではない

### Tree Dired: シンボリックリンク循環の保護
- collectEntries の再帰にdepth上限を設ける
- 循環参照のシンボリックリンクによる StackOverflowError を防止

### Tree Dired: レンダリングの最適化
- applyFaces で formatEntryLine と computeMaxSizeWidth が二重に計算されている
- buildText と applyFaces を統合するか、結果をキャッシュして再利用する

### 非同期バッファ変更時の再描画最適化
- タイマーベースの定期再描画（200ms間隔）は ADR 0096 で導入済み
- 現在はバッファに変更がなくてもスナップショットが生成される
- ダーティフラグやEventNotifier導入で不要な再描画を抑制する最適化が可能

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

### query-replace の Emacs 互換拡張
- `^` キーによる直前マッチへの後退（ADR 0125 で未対応）
- `case-fold-search` / `case-replace` によるケース保持（ADR 0125 で未対応）
- セッション全体を 1 undo 単位にまとめる対応（現状は 1 置換 1 undo）
  - `UndoManager` に `UndoTransaction beginTransaction()` 型の API を足すのが前提

### rectangle 系コマンドの v1 スコープ外対応（ADR 0126）
- `rectangle-number-lines` (`C-x r N`): 矩形各行の左端に連番を挿入
  - プレフィックス引数（`C-u 数字` / フォーマット文字列プロンプト）の基盤が必要
- `string-insert-rectangle`: 文字列を矩形として行毎に挿入する派生
- CUA 互換の rectangle-mark-mode: 矩形選択モード

### UndoManager の失敗時ロールバック機能
- 複数行編集コマンド（`comment-region`, `indent-region`, 矩形コマンド等）で編集途中に `ReadOnlyBufferException` が発生した場合、一部の行だけ変更済みで残る問題がある
- 現状の `UndoManager#withTransaction` は履歴を破棄するがバッファ内容はロールバックしない
- TextChange を逆順に再適用する形でバッファもロールバックするよう拡張する
- これにより「read-only 範囲を跨ぐ編集は全体失敗」を保証できる

### ~~非同期プロンプトを挟むコマンドの undo transaction 対応~~
- ADR 0127 で対応済み（TransactionalCommand 導入）
