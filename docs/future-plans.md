やりたいことを書いておく

## リファクタ系

### TestCommandContextFactoryをbuilder patternにしたい

でかいんじゃ

### モードの役割再考

現在はモードとその他諸々(コマンドとか)が別の定義になっているが、グローバル定義とは別にモード特有のコマンドなどを定義できるようにしたい。

モードはキーバインドと色付けだけではなく、様々なものの名前空間として再定義する。

- コマンド
- facename

#### コマンド

モードごとにcommandregistryを持つ。

- バッファで使われているモードで定義されているコマンドはコマンド名だけでアクセスできる
- それ以外のケースは `mode-prefix.command-name` みたいな感じでFQCNでアクセスする。(区切りは暫定)
  - `python-mode.indent-region` みたいな感じ
- グローバル定義はコマンド名だけでアクセスできるが、バッファ内でオーバーライドしてしまっているケースなどでは  `global.execute-command` などとしてアクセスできる
- 名前解決の優先度は
  - FQCNアクセス
  - バッファローカル
  - グローバル


#### face

いまはFaceName にモード特有の色付けが生えてしまっている。
ここらへんはモード特有の情報であるので、モードに持たせたい。

## 基本

### バッファごとのカレントディレクトリ

find-file, dired なんかだと欲しい機能

## 検索系

### i-search, i-search backwork

ないと意味ないよね

### occur

これもね

### find-grep

あると便利ですね

## 拡張系

### emacsclientみたいなやつ

EDITOR=emacsclient 等とすると便利に使えるので、似たようなことをしたくなる

### zip-mode

tree-dired みたいな感じで読めるといいんじゃない?

### windows.el みたいなやつ

よく使うので欲しいね

### shell-mode, eshell

eshellいるかなあ…。

shell-modeだけでいいんじゃないか。そもそもemacsじゃないからashellみたいになりそうだ。

### 透過的gzip読み込み

たまに欲しい

## 言語系

### LSP連携

### java-mode

### js-mode

### json-mode

js-modeでいいじゃんはある

### yaml-mode

### rst-mode

### ts-mode

### kotlin-mode
