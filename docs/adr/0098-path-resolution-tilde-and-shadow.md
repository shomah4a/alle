# ADR 0098: パス解決における ~ 表示とシャドウパス

## ステータス

採用

## コンテキスト

find-file 等でファイルパスを入力する際、HOME ディレクトリの絶対パスがそのまま表示されるため長くなる。
また、Emacs のようにパス入力中に `~` や `/` を入力してパスの起点を切り替える操作ができない。

## 決定

### 1. FilePathInputPrompter の導入

ファイルパス入力に固有の責務を `FilePathInputPrompter` に集約する。
`InputPrompter` を has-a で保持し、以下を担う。

- 初期値の HOME → `~` 表示変換
- `FilePathCompleter` の生成
- シャドウ検出と `FILE_NAME_SHADOW` face の適用
- 確定結果のシャドウ除去と `~` 展開
- `InputHistory` へのシャドウ除去済み値の追加

### 2. HOME → ~ 表示と展開

`PathResolver` ユーティリティクラスが以下の変換を提供する。

- `collapseTilde`: 絶対パスの HOME 部分を `~` に置換
- `expandTilde`: `~` で始まるパスを HOME の絶対パスに展開
- `findShadowBoundary`: パス入力文字列のシャドウ境界位置を検出

`FilePathCompleter` は純粋にパスの補完候補を生成する責務のみを持つ。
シャドウの認識は `FilePathInputPrompter` 内の `ShadowAwareCompleter` が担い、
有効パスのみを `FilePathCompleter` に渡して候補にシャドウプレフィックスを再付与する。

### 3. シャドウパス表示

- パス入力中に `/~` や `//` を入力すると、それ以前のパスがシャドウ表示される
- `InputPrompter` に `InputUpdateListener` コールバック付きオーバーロードを追加
- `MinibufferInputPrompter` は文字挿入・バックスペース後にコールバックを呼ぶだけ（シャドウの意味は知らない）
- `FilePathInputPrompter` がコールバックで face を適用する

### 4. 責務分離の方針

- `Completer`: 補完候補の生成のみ。シャドウの知識を持たない
- `MinibufferInputPrompter`: ミニバッファの汎用操作。ファイルパス固有の知識を持たない
- `FilePathInputPrompter`: ファイルパス入力の全責務（~、シャドウ、補完の仲介）
- `PathResolver`: パス変換のステートレスユーティリティ

## 影響

- FilePathInputPrompter: 新規。ファイルパス入力の責務を集約
- FilePathInputPrompter.ShadowAwareCompleter: 新規。シャドウ対応の補完ラッパー
- InputUpdateListener: 新規。テキスト変更通知の関数型インターフェース
- InputPrompter: InputUpdateListener 付きオーバーロード追加
- MinibufferInputPrompter: InputUpdateListener の通知、シャドウ固有コード除去
- FindFileCommand: FilePathInputPrompter 経由に変更、~ 処理除去
- SaveBufferCommand: 同上
- TreeDiredCommand: 同上
- FilePathCompleter: シャドウ処理除去、純粋な補完のみ
- Completer: shadowBoundary メソッド除去
- PathResolver: findShadowBoundary 追加
- FaceName: FILE_NAME_SHADOW 追加
- DefaultFaceTheme: FILE_NAME_SHADOW スタイル定義（black_bright）
- EditorCore: FilePathInputPrompter の生成と配布
