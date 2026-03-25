# ADR 0059: スナップショット生成責務のcore層移動

## ステータス

承認

## コンテキスト

ADR 0038 で描画スナップショット（`RenderSnapshot`）を導入し、
ADR 0039 で入力・ロジック・描画の3スレッド分離を実現した。

しかし、スナップショット生成ロジック（`ScreenRenderer.createSnapshot()`）は
alle-tui 層に置かれたままであり、以下の問題がある:

- TUI層がcore層の内部構造（Frame, Window, BufferFacade, SyntaxStyler等）を直接操作している
- WindowLayout計算、スタイリング、カーソル位置計算、モードライン組み立てなど、
  本来core層の責務であるロジックがTUI層に漏洩している
- TUI層からcore層オブジェクトの状態を変異させている（ensurePointVisible等）

ADR 0038 では「TerminalSize等のLanterna依存があるためalle-coreには置かない」としていたが、
Lanterna固有の型（TerminalPosition, TerminalSize）を汎用的な型に置き換えることで
この制約は解消できる。

関連: [ADR 0038](0038-render-snapshot-delta.md), [ADR 0039](0039-render-thread-separation.md)

## 決定

### RenderSnapshotをcore層に移動

`io.github.shomah4a.alle.core.render` パッケージを新設し、
`RenderSnapshot` record群をcore層に配置する。

Lanterna依存の除去:
- `TerminalPosition` → `CursorPosition` record を新設（core.render内）
- `TerminalSize` → `int cols, int rows` で受け渡し

### RenderSnapshotFactoryの新設

スナップショット生成ロジックを `RenderSnapshotFactory` としてcore.renderパッケージに配置する。

```java
RenderSnapshotFactory.create(Frame frame, MessageBuffer messageBuffer, int cols, int rows)
    → RenderSnapshot
```

Frameに直接メソッドを追加しないことで、Frameの責務（ウィンドウツリー管理）を維持する。
MessageBufferはFrameが知るべき対象ではないため、Factoryの引数として渡す。

### 移動するロジック

以下のロジックをScreenRendererからRenderSnapshotFactoryに移動する:

- WindowLayout計算
- ensurePointVisible / ensurePointHorizontallyVisible
- SyntaxStylerによる行スタイリング
- モードライン文字列の組み立て
- カーソル位置の計算
- ミニバッファ/エコーエリアの状態収集

### TUI層の役割

ScreenRendererは `renderSnapshot()` のみを担当する純粋な描画クラスとなる。
EditorThreadは `RenderSnapshotFactory.create()` を呼び出してスナップショットを生成する。

## 影響

- core層にrender関心事のパッケージが追加される
- TUI層からcore層の内部知識への依存が大幅に削減される
- 3スレッド構成（ADR 0039）は維持される
  - ロジックスレッド: Factory経由でスナップショット生成
  - 描画スレッド: ScreenRendererでスナップショット描画
- ADR 0038の「alle-coreに置かない」判断を変更する
