# ADR 0024: kill-buffer コマンドの実装

## ステータス

承認

## コンテキスト

エディタにバッファを閉じる機能がなく、不要になったバッファを削除できない。
Emacs の `kill-buffer` (C-x k) に相当する機能を実装する。

## 決定

### 基本動作
- プロンプトでバッファ名を入力（デフォルト: 現在のバッファ名、BufferNameCompleter 付き）
- 指定バッファを BufferManager から削除する
- 削除対象を表示中の全ウィンドウを別バッファに切り替える

### 切り替え先バッファの選択
- 他のウィンドウで表示されていないバッファを優先選択する
- 該当がなければ `*scratch*` に切り替える

### *scratch* バッファの特別扱い
- `*scratch*` を削除した場合、サイレントで再作成し BufferManager に追加する
- 切り替え先は別バッファがあればそちらを優先する

### previousBuffer の dangling reference 対策
- バッファ削除時に全ウィンドウの `previousBuffer` を走査し、削除対象への参照をクリアする
- `Window.clearPreviousBufferIf(Buffer)` メソッドを追加する

### キーバインド
- C-x k にバインドする

## 影響

- `Window` に `clearPreviousBufferIf` メソッドが追加される
- `KillBufferCommand` が新規作成される
- `Main.java` にコマンド登録とキーバインドが追加される
