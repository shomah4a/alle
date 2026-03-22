# ADR-0055: マルチラインハイライトの導入

## ステータス

提案

## コンテキスト

ADR-0028 でシンタックスハイライトを導入し、行単位のハイライト（LineMatch / PatternMatch）を実装した。
しかし、コードブロック（` ``` `）や複数行コメントなど、複数行にまたがる構文のハイライトには対応していなかった。
ADR-0028 で設計済みの `RegionMatch(Pattern open, Pattern close, Face face)` を実装し、マルチラインハイライトに対応する。

## 決定

### 状態付きハイライト方式

emacs の font-lock に倣い、前行からの状態を引き継ぐ stateful なハイライト方式を採用する。

### HighlightState / HighlightResult

```
record HighlightState(Optional<HighlightRule.RegionMatch> activeRegion)
record HighlightResult(ListIterable<StyledSpan> spans, HighlightState nextState)
```

- `HighlightState.NONE`: リージョン外の初期状態
- `HighlightResult`: 行のハイライト結果と次行に渡す状態のペア

### SyntaxHighlighter の拡張

```
interface SyntaxHighlighter {
    ListIterable<StyledSpan> highlight(String lineText);                        // 既存（抽象）
    HighlightResult highlightLine(String lineText, HighlightState state);       // 新規（デフォルト実装あり）
    HighlightState initialState();                                              // 新規（デフォルト実装あり）
}
```

- `highlight` は抽象メソッドのまま維持し、後方互換を保つ
- `highlightLine` のデフォルト実装は `highlight` を呼び出して `HighlightState.NONE` を返す
- 無限再帰を避けるため、`highlight` から `highlightLine` への委譲は実装クラス側で明示的に行う

### RegionMatch の動作

- open パターンにマッチした位置から行末（または同一行内で close が見つかればそこ）までリージョン Face を適用
- 次行以降は close パターンが見つかるまでリージョン Face を行全体に適用
- 他のルールと同じ covered 配列ベースの優先順位制御に参加する（定義順優先）
- リージョン内では他のルールは無視される（将来の拡張として内部ルール対応の余地は残す）

### ScreenRenderer の状態引き回し

- `createSnapshot()` で各行を処理する際、前行の `HighlightState` を引き継ぐ
- displayStart より前の行は状態計算のみ行い、スパンは破棄する
- パフォーマンス最適化（状態キャッシュ等）は別タスクとする

## 帰結

- コードブロックや複数行コメントなど、行をまたぐ構文のハイライトが可能になる
- 既存の行単位ハイライトとの後方互換を維持する
- displayStart 前の行を毎回処理するため O(N) のコストがかかるが、初期実装では許容する
