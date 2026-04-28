# ADR 0130: redraw-display コマンド（C-l 全画面再描画）

## ステータス

承認

## コンテキスト

ターミナルの画面が崩れた場合（外部プロセスの出力混入、リサイズ不整合など）に回復する手段が存在しない。
Emacs の C-l に相当する全画面強制再描画コマンドが必要である。

現在の描画パイプラインは `RenderThread` が `Screen.RefreshType.DELTA`（差分描画）のみを使用している。
全画面再描画には `RefreshType.COMPLETE` を使用する必要がある。

## 決定

### コマンド配置: alle-tui モジュール

全画面再描画はターミナル固有の関心事であるため、`RedrawDisplayCommand` を `alle-tui` モジュールに配置する。
`alle-core` の `CommandContext` や `CommandLoop` には変更を加えない。

これは `alle-tui` に `Command` 実装を配置する初の前例となる。
`alle-tui` は `alle-core` に依存しており、`Command` インターフェースを実装すること自体にモジュール依存上の問題はない。
ターミナル描画に直接関わるコマンドは今後も `alle-tui` に配置するのが妥当である。

### スレッド間通信: AtomicBoolean フラグ

コマンド実行スレッド（EditorThread）から描画スレッド（RenderThread）への通知に `AtomicBoolean` を使用する。
`Main.java`（alle-app）で `AtomicBoolean` を生成し、コマンドと `EditorRunner` の両方に渡す。

- コマンド: フラグを `true` にセット
- RenderThread: `compareAndSet(true, false)` で消費し、`true` なら `COMPLETE`、`false` なら `DELTA` で描画

`AtomicBoolean` は volatile セマンティクスを持つため、スレッド間の可視性は保証される。

## 結果

- C-l で全画面強制再描画が可能になる
- 既存の差分描画パスへの影響はない（フラグが `false` の場合は従来通り `DELTA`）
- `alle-core` への変更なしにターミナル固有のコマンドを追加できる前例が確立される
