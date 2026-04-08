# ADR 0117: タブ文字のレンダリングと TAB_WIDTH 設定

## ステータス

提案中

## コンテキスト

`DisplayWidthUtil.getDisplayWidth(int codePoint)` はタブ文字 (U+0009) を特別扱いしていないため、タブの表示幅が常に 1 カラムとして計算されていた。結果として以下の不具合が発生する:

- `ScreenRenderer` でタブが 1 カラムの制御文字として描画される
- `RenderSnapshotFactory` のカーソル画面カラム計算がずれる
- `Window#ensureCursorVisible` の水平スクロール判定が誤る
- `VisualLineUtil` の折り返し位置がずれる
- `NextLineCommand` / `PreviousLineCommand` の目標カラム維持がずれる

タブの表示幅はその時点の表示カラムに依存するため (`width = tabWidth - (currentColumn % tabWidth)`)、単一コードポイントから幅を返す既存 API では情報が不足している。

## 決定

### 設定の追加

`EditorSettings` に `TAB_WIDTH` を追加する。デフォルト値は 8。

```java
public static final Setting<Integer> TAB_WIDTH = Setting.of("tab-width", Integer.class, 8);
```

タブの扱いは「タブストップ方式」とする。つまりタブ文字は次の `tabWidth` の倍数カラムまで空白で進める。

**タブストップの基準**: `lineText` 先頭からの絶対カラムを基準とする。視覚行の折り返しが発生しても、タブの展開幅は絶対カラム位置から計算し、折り返しによって展開幅が変わることはない。

### DisplayWidthUtil の API 変更

既存シグネチャは削除し、以下に置き換える (プロジェクトポリシーにより後方互換シムは設けない):

```java
// 文字単体の幅（タブはカラム位置に依存）
static int getDisplayWidth(int codePoint, int currentColumn, int tabWidth);

// オフセットからカラム位置を計算
static int computeColumnForOffset(String lineText, int codePointOffset, int tabWidth);

// カラムを文字境界に丸める
static int snapColumnToCharBoundary(String lineText, int column, int tabWidth);
```

`isFullWidth(int codePoint)` はタブと無関係なので従来どおり残す。

### 呼び出し側の修正方針

タブ幅はバッファ単位の設定として `Buffer#getSettings()` から取得する。呼び出し側は引数として `tabWidth` を受け取る、もしくはバッファから直接解決する。

影響を受けるクラス:

- `alle-core/.../render/RenderSnapshotFactory`
- `alle-core/.../window/Window`
- `alle-core/.../VisualLineUtil`
- `alle-core/.../command/commands/NextLineCommand`
- `alle-core/.../command/commands/PreviousLineCommand`
- `alle-tui/.../tui/ScreenRenderer`
  - `renderLineAt`, `renderLineWithHighlight` のタブ描画
  - `applyRegionHighlight` のリージョン矩形計算

レンダリング時のタブ描画は空白文字で埋める（タブ文字そのものを Screen に書き込まない）。

### ミニバッファのタブ幅フォールバック

`MessageBuffer.getLastMessage()` 経由の表示時はバッファ参照を持たないため、`EditorSettings.TAB_WIDTH` のデフォルト値（8）をフォールバックとして使用する。

### RenderSnapshot への tabWidth 伝搬

`ScreenRenderer` はバッファ設定に直接アクセスしないため、`RenderSnapshot.WindowSnapshot` および `MinibufferSnapshot` に `tabWidth` フィールドを追加し、`RenderSnapshotFactory` が解決した値を持たせる。

## 理由

- タブストップ方式は端末および主要エディタの標準挙動で一貫性がある
- `Buffer#getSettings()` を通す方式は `INDENT_WIDTH` と対称でありプロジェクト内で一貫している
- 後方互換シムを残さないことで「一貫した設計ポリシー」を優先する (general.md)

## 影響

- タブを含む既存ファイルの表示カラム、折り返し、カーソル位置、水平スクロール挙動が変わる
- 現行のユニットテストのうち `DisplayWidthUtil` や折り返し系テストはシグネチャ変更に伴う修正が必要
