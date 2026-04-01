# ADR 0098: パス解決における ~ 表示とシャドウパス

## ステータス

採用

## コンテキスト

find-file 等でファイルパスを入力する際、HOME ディレクトリの絶対パスがそのまま表示されるため長くなる。
また、Emacs のようにパス入力中に `~` や `/` を入力してパスの起点を切り替える操作ができない。

## 決定

### 1. HOME → ~ 表示と展開

`PathResolver` ユーティリティクラスを導入し、以下の変換を提供する。

- `collapseTilde`: 絶対パスの HOME 部分を `~` に置換して表示用文字列を生成する
- `expandTilde`: `~` で始まるパスを HOME の絶対パスに展開する

FindFileCommand の初期値表示で HOME を `~` に置換し、パス解決時に `~` を展開する。
FilePathCompleter は PathResolver を利用して ~ パスの補完候補を生成する。
FilePathCompleter のコンストラクタシグネチャは変更せず、呼び出し元で展開する設計とする。

InputHistory には `~` 付きの表示形式で保存する（ユーザーにとって自然な形式）。

### 2. シャドウパス表示

Emacs の file-name-shadow-mode に相当する機能を実装する。

- パス入力中に `~` や `/` を入力すると、それ以前のパスがシャドウ（薄く表示）される
- シャドウ部分はミニバッファ内のテキストとして実際に保持し、face で視覚的に区別する
- 確定時にはシャドウ部分を除去し、有効パスのみを返す
- バックスペースでシャドウ文字を消すとシャドウが解除される

シャドウ検出は `Completer` インターフェースにデフォルトメソッドとして追加し、
MinibufferInputPrompter の汎用性を維持する。

## 影響

- FindFileCommand: 初期値の ~ 表示、パス解決時の ~ 展開
- FilePathCompleter: ~ パスの補完候補生成
- PathResolver: 新規ユーティリティクラス
- FaceName: FILE_NAME_SHADOW の追加
- Completer: シャドウ境界判定のデフォルトメソッド追加
- MinibufferInputPrompter: シャドウ face の適用と確定時の除去
- DefaultFaceTheme: SHADOW face のスタイル定義
