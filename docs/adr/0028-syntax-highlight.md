# ADR-0028: シンタックスハイライトの導入

## ステータス

提案

## コンテキスト

> **注**: マルチラインハイライト（RegionMatch）の実装については [ADR-0055](0055-multiline-highlight.md) を参照。

エディタにシンタックスハイライト機能を導入する。
モードに紐づく機能として設計し、スクリプト拡張でモード定義しやすい宣言的な構造にする。
組み込みモードは最小限にとどめる。

## 決定

### レイヤー構成

- Face/HighlightRule/SyntaxHighlighter: core層（Lanterna非依存）
- FaceResolver: tui層（Named色→Lanterna TextColor/SGRへの変換）

### Face

色の指定はNamed色のみとする。RGB直指定はダークモード等での破綻を防ぐために禁止する。
テーマ対応はFaceResolverのNamed→色マッピングテーブルの差し替えで実現する。

```
record Face(String foreground, String background, ImmutableSet<FaceAttribute> attributes)
```

- foreground/background: Named色名（"red", "blue", "default"等）
- FaceAttribute: enum { BOLD, ITALIC, UNDERLINE }

### ハイライトルール

宣言的なデータとして定義し、スクリプトからルールリストを渡すだけでモードを定義可能にする。

```
sealed interface HighlightRule
  record LineMatch(Pattern pattern, Face face)     // 行全体にマッチ
  record PatternMatch(Pattern pattern, Face face)  // 部分パターンマッチ
  record RegionMatch(Pattern open, Pattern close, Face face) // 開始/終了パターン
```

### RegexHighlighter

HighlightRuleのリストを受け取り、行テキストからStyledSpanリストを生成する汎用実装。
各モードはルール定義のみ行い、ハイライト処理はRegexHighlighterに委譲する。

### MajorMode.highlighter()

`default Optional<SyntaxHighlighter> highlighter()` をMajorModeに追加する。
defaultメソッドとしてOptional.empty()を返すため、既存モードへの影響なし。

### 最初のターゲット

Markdownモード。行単位で処理しやすい構文が多く、初期実装に適している。
コードブロック（` ``` `）は行単位処理では扱えないため初期スコープ外とする。

## 帰結

- ハイライトルールが宣言的なデータなので、将来のスクリプト拡張で容易にモード定義可能
- Named色のみとすることでテーマ切り替えがFaceResolver側の差し替えで完結する
- コードポイント/charオフセットの変換はRegexHighlighter内で吸収する
