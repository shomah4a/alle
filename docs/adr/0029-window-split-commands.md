# ADR-0029: ウィンドウ分割コマンド (C-x 1/2/3/0)

## ステータス

提案

## コンテキスト

Emacsスタイルのウィンドウ分割操作 C-x 1, C-x 2, C-x 3, C-x 0 を実装する。
WindowTree/Frame層にはsplit/remove/nextWindowが既に存在するが、ScreenRendererは単一ウィンドウ描画のみ対応している。

## 決定

### コマンド定義

| キーバインド | コマンド名 | 動作 |
|---|---|---|
| C-x 2 | split-window-below | アクティブウィンドウを上下分割（HORIZONTAL）、同一バッファを表示 |
| C-x 3 | split-window-right | アクティブウィンドウを左右分割（VERTICAL）、同一バッファを表示 |
| C-x 0 | delete-window | アクティブウィンドウを閉じる（最後の1つは閉じない） |
| C-x 1 | delete-other-windows | アクティブウィンドウ以外を全て閉じる |

### Emacs互換の挙動

- C-x 2/3: 分割後、カーソルは元のウィンドウに留まる
  - 現在の `Frame.splitActiveWindow()` はアクティブウィンドウを新ウィンドウに切り替えてしまうため、コマンド側で元に戻す
- C-x 0: 最後のウィンドウは閉じない（Frame.deleteWindowがfalseを返す）
- C-x 1: ツリーをアクティブウィンドウのLeafに直接置換する
  - `Frame.deleteOtherWindows()` メソッドを新規追加

### ScreenRenderer分割描画

矩形領域ベースの描画に段階的に移行する。

#### レイアウト計算の分離

`WindowLayout` クラスを導入し、WindowTreeから各ウィンドウへの矩形割り当てを計算する。ScreenRendererから分離することでユニットテストを可能にする。

```
record Rect(int top, int left, int width, int height) {}
```

#### 描画レイアウト

各ウィンドウの矩形内:
- 行 top ~ top+height-2: バッファ表示エリア
- 行 top+height-1: モードライン

画面全体:
- 行 0 ~ rows-2: ウィンドウツリー領域
- 行 rows-1: ミニバッファ（分割と独立、常に画面最下行）

垂直分割時はウィンドウ間に1カラムの縦線セパレータを描画する。

## 実装フェーズ

### Phase 1: コマンドとFrame拡張（core層）

- `Frame.deleteOtherWindows()` 追加
- SplitWindowBelowCommand, SplitWindowRightCommand, DeleteWindowCommand, DeleteOtherWindowsCommand を作成
- テスト追加
- Main.javaにコマンド登録とキーバインド追加

この時点では分割描画は未対応だが、コマンドとしては動作する（最初のウィンドウのみ表示）。

### Phase 2: レイアウト計算（core層）

- `Rect` レコード追加
- `WindowLayout` クラス追加: WindowTree + 利用可能領域 → 各ウィンドウのRect割り当て
- セパレータ幅の考慮（垂直分割時に1カラム消費）
- テスト追加（off-by-oneエラーの検出）

### Phase 3: ScreenRenderer分割描画対応（tui層）

- renderWindowにRectパラメータを追加
- renderLine系メソッドにtop/leftオフセットを追加
- positionCursorにオフセットを追加
- ensurePointVisibleにウィンドウ固有の行数を渡す
- 各ウィンドウのモードライン描画
- セパレータ描画
- アクティブウィンドウのみにカーソル表示

## リスクと対策

- **ScreenRenderer改修のリグレッション**: Phase 3ではまず単一ウィンドウでの矩形描画が既存と同一結果になることを確認してから、複数ウィンドウ描画を実装する
- **splitActiveWindowの仕様差異**: コマンド側でアクティブウィンドウを元に戻す
- **off-by-oneエラー**: WindowLayoutにユニットテストを充実させる
