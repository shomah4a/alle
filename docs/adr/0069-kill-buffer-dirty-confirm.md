# ADR-0069: kill-buffer のdirtyバッファ確認

## ステータス

承認済み

## コンテキスト

kill-buffer コマンドでdirtyバッファを削除する際、未保存の変更が失われることをユーザーに通知せず削除していた。
意図しないデータ損失を防ぐため、確認ダイアログを追加する必要がある。

## 決定

### 確認プロンプトの仕様

- dirtyバッファを削除する際に確認プロンプトを表示する
- プロンプトメッセージ: `"Buffer <name> modified; kill anyway? (yes, no, save and kill) "`
- 選択肢（Tab補完付き）:
  - `yes` — 保存せず削除
  - `no` — 削除を中止
  - `save and kill` — 保存してから削除
- dirtyでないバッファは従来通り確認なしで削除する

### save and kill の制約

- ファイルパスが未設定のバッファでは save and kill を選択してもメッセージを表示して中止する
- 先に save-buffer でファイルパスを設定してから再実行する運用とする

### 実装方針

- `killBuffer` メソッドの戻り値を `CompletableFuture<Void>` に変更する
- `execute` 内の `thenAccept` を `thenCompose` に変更する
- `BufferIO` をコンストラクタ注入し、save 機能を利用可能にする

## 影響

- `KillBufferCommand`: dirty確認ロジック追加、コンストラクタ変更
- `EditorCore`: コマンド登録時に `BufferIO` を渡す
