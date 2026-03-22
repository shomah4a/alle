# ADR-0055: マルチラインスタイリングの導入

## ステータス

承認

## コンテキスト

ADR-0028 でシンタックスハイライトを導入し、行単位のスタイリング（LineMatch / PatternMatch）を実装した。
しかし、コードブロック（` ``` `）や複数行コメントなど、複数行にまたがる構文のスタイリングには対応していなかった。
ADR-0028 で設計済みの `RegionMatch(Pattern open, Pattern close, Face face)` を実装し、マルチラインスタイリングに対応する。

また、ハイライト（色付け）だけでなく文字装飾（bold, italic, strikethrough等）も統一的に扱うため、
highlight パッケージを styling パッケージにリネームした。

## 決定

### パッケージリネーム

`io.github.shomah4a.alle.core.highlight` → `io.github.shomah4a.alle.core.styling`

CSSのstyleプロパティと同様に、色付けと文字装飾を統一的に「スタイリング」として扱う。

主なクラス名変更:
- `SyntaxHighlighter` → `SyntaxStyler`
- `RegexHighlighter` → `RegexStyler`
- `HighlightRule` → `StylingRule`
- `HighlightState` → `StylingState`
- `HighlightResult` → `StylingResult`
- `MajorMode.highlighter()` → `MajorMode.styler()`

Face, FaceAttribute, StyledSpan はクラス名変更なし。

### 状態付きスタイリング方式

emacs の font-lock に倣い、前行からの状態を引き継ぐ stateful なスタイリング方式を採用する。

### StylingState / StylingResult

```
record StylingState(Optional<StylingRule.RegionMatch> activeRegion)
record StylingResult(ListIterable<StyledSpan> spans, StylingState nextState)
```

- `StylingState.NONE`: リージョン外の初期状態
- `StylingResult`: 行のスタイリング結果と次行に渡す状態のペア

### SyntaxStyler の拡張

```
interface SyntaxStyler {
    ListIterable<StyledSpan> styleLine(String lineText);                           // 既存（抽象）
    StylingResult styleLineWithState(String lineText, StylingState state);         // 新規（デフォルト実装あり）
    StylingState initialState();                                                   // 新規（デフォルト実装あり）
}
```

- `styleLine` は抽象メソッドのまま維持し、後方互換を保つ
- `styleLineWithState` のデフォルト実装は `styleLine` を呼び出して `StylingState.NONE` を返す
- 無限再帰を避けるため、`styleLine` から `styleLineWithState` への委譲は実装クラス側で明示的に行う

### RegionMatch の動作

- open パターンにマッチした位置から行末（または同一行内で close が見つかればそこ）までリージョン Face を適用
- 次行以降は close パターンが見つかるまでリージョン Face を行全体に適用
- 他のルールと同じ covered 配列ベースの優先順位制御に参加する（定義順優先）
- リージョン内では他のルールは無視される（将来の拡張として内部ルール対応の余地は残す）

### ScreenRenderer の状態引き回し

- `createSnapshot()` で各行を処理する際、前行の `StylingState` を引き継ぐ
- displayStart より前の行は状態計算のみ行い、スパンは破棄する
- パフォーマンス最適化（状態キャッシュ等）は別タスクとする

### FaceAttribute.STRIKETHROUGH の追加

取り消し線装飾に対応するため `FaceAttribute.STRIKETHROUGH` を追加し、Lanterna の `SGR.CROSSED_OUT` にマッピングした。

## 帰結

- コードブロックやHTMLコメントなど、行をまたぐ構文のスタイリングが可能になる
- 既存の行単位スタイリングとの後方互換を維持する
- displayStart 前の行を毎回処理するため O(N) のコストがかかるが、初期実装では許容する
- stylingパッケージへのリネームにより、色付けと文字装飾を統一的な概念として扱えるようになった
