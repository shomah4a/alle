# ADR 0131: shell-command コマンド

## ステータス

Accepted

## コンテキスト

Emacsの `shell-command` (M-!) に相当するコマンドがない。
ユーザーがエディタ内からシェルコマンドを実行し、結果を確認できるようにしたい。

## 決定

### コマンド仕様

- コマンド名: `shell-command`
- キーバインド: M-!
- M-x からも呼び出し可能（CommandRegistry に登録するため自動的に対応）

### 実行フロー

1. ミニバッファで "Shell command: " プロンプトを表示
2. ユーザーがコマンドを入力・確定
3. `*Shell Command Output*` バッファを取得または作成し、内容をクリア
4. 画面を下に分割して出力バッファを表示（既にウィンドウがあればそちらにフォーカス）
5. `ShellCommandExecutor` で非同期実行
6. stdout/stderr の各行をコールバックからバッファに直接書き込み
7. stderr は WARNING face で色分け
8. プロセス完了時に exit code を表示

### 出力のバッファ書き込み

`ShellCommandExecutor` の `onStdoutLine` / `onStderrLine` コールバックから直接バッファに書き込む。
`DefaultShellCommandExecutor` は stdout をメインスレッド、stderr を別スレッドでブロッキングリードする実装になっており、それぞれのコールバック内で `atomicOperation` 経由でバッファに書き込むことでスレッドセーフに出力を反映する。

### 作業ディレクトリ

アクティブバッファのデフォルトディレクトリを使用する。
ファイルに紐づいていない場合は `Path.of("").toAbsolutePath()` をフォールバックとする。

### バッファ名

`*Shell Command Output*` — TreeDiredShellCommand と共用する（Emacs と同様）。

### 排他制御

AtomicBoolean で実行中フラグを管理し、実行中に再度呼ばれた場合はメッセージを表示して拒否する。

### バッファ書き込みユーティリティの共通化

TreeDiredShellCommand の `appendText`, `appendStyledText`, `initOutputBuffer` を `ShellOutputBufferHelper` クラスに抽出し、ShellCommandCommand と共用する。
TreeDiredShellCommand にもバッファフラッシュ（実行前のクリア）処理を追加する。

## 影響

- 新規: `ShellCommandCommand.java`, `ShellOutputBufferHelper.java`
- 変更: `EditorCore.java`（コマンド登録 + M-! キーバインド）, `TreeDiredShellCommand.java`（ユーティリティ抽出）
