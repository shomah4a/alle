# ADR 0119: GapTextModel に改行位置キャッシュを導入する

## ステータス

提案中

## コンテキスト

async profiler によるプロファイリングの結果、GapTextModel の行操作メソッド (`lineCount`, `lineText`, `lineStartOffset`, `lineIndexForOffset`) が最大のボトルネックであることが判明した。

現状これらのメソッドは毎回 `GapModel.charAt` を1文字ずつ呼び出して改行を走査しており、すべて O(n) の計算量を持つ。`charAt` は内部で `toPhysical` による論理→物理アドレス変換を行うため、呼び出し回数に比例してオーバーヘッドが蓄積する。プロファイルでは `GapModel.toPhysical` が全サンプルの 14.6% を占めていた。

特に `lineText` や `lineStartOffset` はバリデーションのために `lineCount()` を呼んでおり、1回の呼び出しで全文走査が2回以上発生するケースがある。

## 決定

GapTextModel に改行文字の char offset をキャッシュする `MutableIntList` フィールドを導入する。

### キャッシュの設計

- キャッシュは改行文字 (`\n`) の **論理 char offset** のリストとする
- `insert` / `delete` 時にキャッシュを差分更新する
- `GapTextModel(GapModel)` コンストラクタでは渡された GapModel を全文走査してキャッシュを初期構築する

### 計算量の改善

| メソッド | 変更前 | 変更後 |
|---|---|---|
| `lineCount()` | O(n) | O(1) |
| `lineText(i)` | O(n) | O(行の長さ) |
| `lineStartOffset(i)` | O(n) | O(行の先頭までのchar数) ※ |
| `lineIndexForOffset(offset)` | O(n) | O(log L + charOffset変換) |

※ `charOffsetToCodePointIndex` のコスト（先頭からの codePoint 走査）は今回のスコープでは削減対象外とする。

### GapModel の拡張

`charAt` ループを回避するため、GapModel に `indexOf(char c, int fromIndex, int toIndex)` メソッドを追加する。gap をまたいだ文字検索を物理配列上で直接行う。

### スコープ外

- `charOffsetToCodePointIndex` の最適化（codePoint offset キャッシュ等）は別タスクとする
- `Character.UnicodeBlock.of` / `isCharCJK` の最適化は lanterna 側の問題であり別タスクとする

## 影響

- `GapModel.java`: `indexOf` メソッド追加（既存 API 変更なし）
- `GapTextModel.java`: 内部実装変更（`TextModel` インターフェース変更なし）
- キャッシュの差分更新は `insert` / `delete` のパスで行われるため、`TextBuffer` 経由の undo/redo も含めすべての変更パスでキャッシュが正しく更新される

## 将来の拡張候補

- 巨大ファイル（数万行超）でのキャッシュ offset シフトコストが問題になる場合、B-Tree ベースの行インデックスへの移行を検討する
- codePoint offset のキャッシュ（行ごとの codePoint 開始 offset を保持）により `lineStartOffset` / `lineIndexForOffset` をさらに高速化可能
