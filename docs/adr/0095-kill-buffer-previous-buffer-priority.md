# ADR 0095: kill-buffer 後のバッファ切り替えで previousBuffer を優先する

## ステータス

承認

## コンテキスト

> **注**: 本 ADR の判断は [ADR 0101](0101-window-buffer-history.md) で拡張された。previousBuffer の単一参照から MRU リストへの変更。

kill-buffer でバッファを削除した後、切り替え先は `findReplacementBuffer` によってバッファリストの順序で決定されていた。
このため、削除後は大抵 `*scratch*` など先頭付近のバッファに切り替わり、ユーザーの作業文脈が途切れる。

一方、switch-buffer コマンドでは `Window.previousBuffer`（直前に表示していたバッファ）をデフォルト候補として提示しており、ユーザーの作業文脈に沿った切り替えができている。

## 決定

kill-buffer 後のバッファ切り替えにおいて、各ウィンドウの `previousBuffer` を優先的に使用する。
`previousBuffer` が利用できない場合（null、または kill 対象自身の場合）は、従来の `findReplacementBuffer` にフォールバックする。

### 切り替え優先順位

1. 当該ウィンドウの `previousBuffer`（kill 対象でないこと）
2. 他のウィンドウで表示されていないバッファ（従来ロジック）
3. kill 対象以外の任意のバッファ（従来ロジック）

### 複数ウィンドウの扱い

各ウィンドウが独立した `previousBuffer` を持つため、ウィンドウごとに異なる切り替え先になりうる。
これは switch-buffer と同じ設計方針であり、各ウィンドウが自身の履歴に基づいて動作する。

## 結果

- ユーザーの作業文脈に沿ったバッファ切り替えが実現される
- switch-buffer と kill-buffer で一貫した previousBuffer の活用ができる
- previousBuffer が他ウィンドウで表示中のバッファを指す場合、同じバッファが複数ウィンドウに表示される可能性がある（switch-buffer と同じ挙動）
