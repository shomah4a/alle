# ADR-0022: other-window / switch-buffer コマンド

## ステータス

承認済み

## コンテキスト

複数ウィンドウ間の移動とバッファの切り替えが必要。
Emacs の C-x o (other-window) と C-x C-b (switch-to-buffer) に相当する機能を実装する。
本プロジェクトでは C-x C-o と C-x C-b にバインドする。

## 決定

### other-window (C-x C-o)

- WindowTree に全ウィンドウを深さ優先で列挙する `windows()` メソッドを追加
- Frame に `nextWindow()` メソッドを追加し、現在のアクティブウィンドウの次のウィンドウに循環的に切り替える
- ミニバッファアクティブ中でもツリー内のウィンドウに移動する（Emacs 準拠）
- ウィンドウが 1 つしかない場合は何もしない

### switch-buffer (C-x C-b)

- ミニバッファでバッファ名を入力し、BufferManager から検索して切り替える
- FindFileCommand と同じパターンで `window.setBuffer()` を使用
- 見つからない場合は何もしない

## 結果

- ウィンドウ分割時に C-x C-o で移動可能になる
- C-x C-b で任意のバッファに切り替え可能になる
