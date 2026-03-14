# ADR-0009: Frame/Windowモデル

## ステータス

承認済み

## コンテキスト

エディタの画面構成として、Emacsのようにウィンドウ分割やミニバッファを実現する必要がある。
また、同一バッファを複数のウィンドウで表示した場合に独立したカーソル位置を持てる設計が求められる。

## 決定

### WindowはBufferに対するビュー

- Windowはバッファへの参照と、ビュー固有の状態（カーソル位置、選択状態、スクロール位置）を持つ
- Bufferはテキストデータ、ファイルパス、ダーティフラグのみを持ち、カーソル位置は持たない
- カーソル依存の編集操作（挿入、バックスペース等）はWindow側の責務とする

### WindowTree（immutable）

- ウィンドウの分割状態をimmutableな木構造で表現する
- sealed interface で Leaf（単一Window）と Split（水平/垂直分割）を定義する
- 分割・削除のたびに新しいツリーを生成する
- 最後の1つのWindowは削除不可（Emacsに倣う）

```java
sealed interface WindowTree {
    record Leaf(Window window) implements WindowTree {}
    record Split(Direction direction, double ratio,
                 WindowTree first, WindowTree second) implements WindowTree {}
}
```

### Frame

- Frameはmutableで、内部にWindowTreeを持つ
- 分割・削除操作時にWindowTreeを作り直して差し替える
- Minibuffer WindowはFrame固定で保持し、WindowTreeの分割対象外とする
- アクティブWindowの管理はFrameの責務

```
Frame
├── WindowTree (root) ← immutable、操作のたびに差し替え
│   ├── Leaf(Window A) → Buffer "foo.java"
│   └── Split
│       ├── Leaf(Window B) → Buffer "bar.txt"
│       └── Leaf(Window C) → Buffer "baz.rs"
└── Minibuffer Window → Minibuffer (Buffer) ※固定
```

## 帰結

- 同一バッファを複数ウィンドウで表示しても、カーソル位置が独立する
- WindowTreeがimmutableなため、分割状態の整合性が保たれやすい
- Bufferの責務が軽くなり、テキストデータとメタ情報の管理に集中できる
- 既存のBufferからpointおよびカーソル依存メソッドを削除する破壊的変更が必要
