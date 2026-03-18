# ADR-0030: 水平スクロール

## ステータス

提案

## コンテキスト

カーソルが行末方向に画面右端を超えて移動した場合、表示が水平方向にスクロールしない。
現在 `Window` には垂直スクロール用の `displayStartLine` のみが存在し、水平方向のスクロールオフセットがない。
`ScreenRenderer` の描画メソッドは常に行頭（カラム0）から描画を開始しており、カーソルが画面外に出てしまう。

## 決定

### Unicode表示幅計算の core 層移動

`ScreenRenderer` にある `getDisplayWidth` / `isFullWidth` / `computeColumnForOffset` は
Unicode 文字幅に関する知識であり、TUI固有ではない。
`Window.ensurePointHorizontallyVisible()` で利用するため、core 層の `DisplayWidthUtil` クラスに移動する。

### Window への水平スクロール状態追加

- `displayStartColumn` フィールドを追加（表示カラム単位、コードポイント単位ではない）
- `ensurePointHorizontallyVisible(int visibleColumns)` メソッドを追加
  - カーソルのカラム位置が `displayStartColumn + visibleColumns` 以上なら右スクロール
  - カーソルのカラム位置が `displayStartColumn` 未満なら左スクロール

### ScreenRenderer の描画修正

- `renderLineAt` / `renderLineWithHighlight`: `displayStartColumn` 分のカラムをスキップして描画
- `positionCursorInRect`: カーソルカラムから `displayStartColumn` を減算
- `positionMinibufferCursor`: 同様に対応

## 実装フェーズ

### Phase 1: DisplayWidthUtil の導入（core層）

- `DisplayWidthUtil` クラスを core に作成し、`getDisplayWidth` / `isFullWidth` / `computeColumnForOffset` を移動
- `ScreenRenderer` からの参照を `DisplayWidthUtil` に変更
- テスト追加
- ビルド確認

### Phase 2: Window への水平スクロール追加（core層）

- `Window` に `displayStartColumn` フィールドを追加
- `ensurePointHorizontallyVisible(int visibleColumns)` を実装
- テスト追加

### Phase 3: ScreenRenderer の水平スクロール対応（tui層）

- 描画メソッドに水平オフセット引数を追加し、スキップ描画を実装
- カーソル位置計算に水平オフセットを反映
- `render()` から `ensurePointHorizontallyVisible()` を呼び出し

## リスクと対策

- **表示幅計算のレイヤー移動**: 純粋関数なので移動リスクは低い。既存テストで動作確認する
- **全角文字境界**: `displayStartColumn` が全角文字の中間を指す場合の扱い。描画時にスキップ中に全角文字の途中で止まった場合はその文字を描画しない
