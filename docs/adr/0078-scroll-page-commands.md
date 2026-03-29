# ADR 0078: ページスクロールコマンド (scroll-up / scroll-down)

## ステータス

承認済み

## コンテキスト

Emacsのページスクロール操作 `C-v` (scroll-up) と `M-v` (scroll-down) を実装する。
これらのコマンドはウィンドウの表示行数に基づいてページ単位でスクロールするため、
コマンド実行時にウィンドウの表示可能サイズを知る必要がある。

現状、`Window` は表示可能サイズを保持しておらず、レイアウト計算は `RenderSnapshotFactory` で行われ、
結果は描画時にのみ使用されている。

## 決定

### ViewportSize レコードの導入

`Window` の表示可能領域サイズを表す `ViewportSize` レコードを導入する。
行数（rows）と列数（columns）をまとめて管理する。

```java
public record ViewportSize(int rows, int columns) {}
```

### Window への保持

`Window` に `volatile` な `ViewportSize` フィールドを追加する。
描画スレッド（`RenderSnapshotFactory`）が書き込み、コマンドスレッド（`CommandLoop`）が読み取るため、
`volatile` でスレッドセーフを確保する。

### RenderSnapshotFactory での更新

`RenderSnapshotFactory` がレイアウト計算後に `Window.setViewportSize()` を呼び出して
表示可能サイズを更新する。既存の `ensurePointVisible(bufferRows)` /
`ensurePointHorizontallyVisible(rect.width())` の呼び出し箇所で同時に設定する。

### コマンド実装

- `ScrollUpCommand` (scroll-up / C-v): ページ下方向にスクロール
- `ScrollDownCommand` (scroll-down / M-v): ページ上方向にスクロール

Emacsと同様に2行のオーバーラップを残す。
`viewportSize` が未設定（rows=0）の場合は何もしない。

## 影響

- `Window` にフィールド1つ追加
- `RenderSnapshotFactory` に setter 呼び出し1箇所追加
- 既存の `ensurePointVisible` / `ensurePointHorizontallyVisible` の引数渡しパターンは変更しない
