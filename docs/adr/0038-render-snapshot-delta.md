# ADR 0038: 描画スナップショットと差分描画

## ステータス

承認（RenderSnapshotの配置先について [ADR 0059](0059-core-render-snapshot.md) で判断変更）

## コンテキスト

ScreenRenderer が毎フレーム `screen.clear()` → 全描画 → `screen.refresh()` を行っており、
表示が多くなると再描画でちらつきが発生する。
将来的な描画スレッド分離に備え、描画データのスナップショット化も必要。

Lanterna には ダブルバッファリング + `RefreshType.DELTA` による差分描画の仕組みがある。
`screen.clear()` を呼ばなければ、Lanterna が前フレームとの差分だけを端末に送信できる。

## 決定

### ScreenRenderer のリファクタ

`render(Frame)` を以下の2段階に分割する:

1. `createSnapshot(Frame, TerminalSize)` → `RenderSnapshot`
   - レイアウト計算
   - ensurePointVisible / ensurePointHorizontallyVisible
   - 表示範囲の行テキスト取得
   - カーソル位置計算
   - モードライン情報収集
   - ミニバッファ状態収集

2. `renderSnapshot(RenderSnapshot)` → 画面描画
   - screen.clear() を呼ばない
   - ウィンドウ領域全体を空白で初期化してから各ウィンドウ・セパレータを描画
   - `screen.refresh(RefreshType.DELTA)` で差分描画

### RenderSnapshot

描画に必要な全情報を immutable に持つ record 群。alle-tui パッケージ内に配置。
TerminalSize 等の Lanterna 依存があるため alle-core には置かない。

### 残像対策

`screen.clear()` を廃止する代わりに、ウィンドウ領域全体（row 0〜rows-2）とミニバッファ行を
描画前に空白で初期化する。テキスト変更のない行は前フレームと同じ内容を書くため、
Lanterna の DELTA が差分なしと判定し端末への送信をスキップする。

### 実装順序

1. renderLineAt 等に行末空白埋めを先行実装（clear() 併存のまま）
2. RenderSnapshot record 群の定義
3. createSnapshot / renderSnapshot への分割（clear() 残す）
4. clear() 除去 + RefreshType.DELTA 切り替え

## 影響

- ScreenRenderer 内部のリファクタのみ。render(Frame) のシグネチャは維持
- Main.java の呼び出し側に変更なし
- 描画スレッド分離は別タスク
