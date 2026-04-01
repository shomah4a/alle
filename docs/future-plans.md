やりたいことを書いておく

## リファクタ系

### TestCommandContextFactoryをbuilder patternにしたい

でかいんじゃ

## 基本

### パス解決系

HOME は ~ として表現したい。ちょっと長い。
あと、find-file などのときに ~ とか / を入力したらemacsみたいに動作させたい

find-file で `/aaaaa/` となっているときに ~ と入力すると
`{/aaaaa/} ~` となってHOMEからのパスになる。
~ を消すと `/aaaaa/` に戻る。

同様に `/aaaa/` となっている時に / を入力すると
`{/aaaa/} /` となってrootからのパスになる。
/ を消すと `/aaaa/` に戻る。

みたいなやつ

## 検索系

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
