# ADR 0083: Tree Dired マーク機能およびファイル操作コマンド

## ステータス

採用

## コンテキスト

Tree Dired（ADR 0082）はディレクトリのツリー表示とファイルオープン・ディレクトリ展開/折り畳みの機能を提供しているが、Emacs の dired にあるようなマーク機能やファイル操作（コピー・移動・削除・chown・chmod）が未実装である。

## 決定

### マーク機能

- `TreeDiredModel` に `MutableSet<Path> markedPaths` を追加し、マーク状態を管理する
- `TreeDiredEntry` に `isMarked` フィールドを追加し、Renderer にマーク状態を一貫して渡す
- 表示上は行頭の `"  "` をマーク時 `"* "` に切り替える（同文字数でオフセット計算に影響しない）
- マーク済みエントリには `DIRED_MARKED` face を適用する

### キーバインド

| キー | コマンド | 動作 |
|------|---------|------|
| m | tree-dired-mark | カーソル行をマーク、次行に移動 |
| u | tree-dired-unmark | カーソル行のマーク解除、次行に移動 |
| t | tree-dired-toggle-mark | C-SPCリージョンがあれば範囲内をトグル、なければカーソル行をトグル |
| f | tree-dired-find-file-or-toggle | 既存のEnterと同じ動作 |
| C | tree-dired-copy | コピー |
| R | tree-dired-rename | リネーム（mv） |
| D | tree-dired-delete | 削除（rm） |
| M | tree-dired-chmod | パーミッション変更 |
| O | tree-dired-chown | オーナー変更 |

### ファイル操作コマンドの対象解決

マーク済みエントリがあればそれを対象とし、なければカーソル行のエントリを対象とする。
この解決ロジックは `TreeDiredEntryResolver.resolveTargets()` として共有する。

### ファイルシステム操作の副作用外部化

`FileOperations` インターフェースを定義し、実装を注入する。
`DirectoryLister` と同じパターンで副作用を外部化し、テスタビリティを確保する。

### ファイル操作後のリフレッシュ

C/R/D/O/M の各コマンドは操作完了後に `TreeDiredBufferUpdater.update()` を呼び、バッファの再描画を行う。
操作成功後はマークもクリアする。

### ディレクトリに対する再帰的操作の確認

- **copy**: ディレクトリが含まれる場合は `Copy recursive? (y/n):` で確認。y で再帰コピー、n でキャンセル
- **delete**: ディレクトリが含まれる場合は `(r)ecursive / (f)iles only / (n)o:` の3択。r で再帰削除、f でファイルのみ削除（マーク対象のうちディレクトリエントリをスキップ）、n でキャンセル
- **rename**: ディレクトリを含む場合も特別な確認なし（`Files.move` がそのまま処理する）

### toggle (t) の仕様

Emacs の dired では t は全エントリのマーク/アンマークをトグルするが、本実装では以下とする:
- C-SPC で範囲選択されている場合: 範囲内のエントリのマーク/アンマークをトグル
- 範囲選択がない場合: カーソル行のみトグル

## 影響

- TreeDiredModel, TreeDiredEntry, TreeDiredRenderer に変更が入る
- 新規コマンドクラスを複数追加する
- FileOperations インターフェースと実装クラスを追加する
- EditorCore でのコマンド登録が増加する
