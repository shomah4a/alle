# ADR 0135: isearch / occur の smart-case 検索

## ステータス

承認

## コンテキスト

インクリメンタルサーチ (`isearch-forward` / `isearch-backward`) と `occur` は現状すべて
`String.indexOf` / `String.lastIndexOf` ベースで動作しており、常に case sensitive な検索を行う。

Emacs では `case-fold-search` (デフォルト `t`) と検索文字列中の大文字の有無により
以下のように挙動が変わる:

- `case-fold-search = t` かつ クエリが全て小文字 → case insensitive
- `case-fold-search = t` かつ クエリに大文字を含む → case sensitive
- `case-fold-search = nil` → 常に case sensitive

ユーザーから前者 2 つ (smart-case) を導入したい要望があった。

ADR 0134 (補完の case-insensitive 化) では smart-case を「将来課題」として明示的に見送り、
boolean 設定 (`completion-ignore-case`) のみ導入していた。本 ADR は補完とは別系統として
isearch / occur に smart-case を導入する。

## 決定

### 適用対象

- `isearch-forward` (C-s)
- `isearch-backward` (C-r)
- `occur`

### 判定ロジック

クエリに 1 つでも「大文字またはタイトルケース文字」を含む code point があれば case sensitive、
それ以外は case insensitive とする。

```java
public static boolean containsUpperCase(String query) {
    return query.codePoints().anyMatch(cp -> Character.isUpperCase(cp) || Character.isTitleCase(cp));
}
```

タイトルケース文字 (合字 `ǅ` など) も大文字扱いする。Emacs の `isearch-no-upper-case-p` と
完全一致するわけではないが、JDK の `Character` API に準拠した判定とする。

### case folding 方針

`String.regionMatches(true, ...)` 準拠の Locale 非依存 case folding を採用する。

- BMP 内の単一 char ↔ 単一 char 対応のケース折りに限定
- Turkish-I (`İ` ↔ `i`) など Locale 依存の case folding は非対応
- `ß` ↔ `SS` のような 1 文字 ↔ 複数文字対応 (Unicode full case folding) は非対応
- ハイライト幅は `query.codePoints().count()` 固定で扱うため、長さが変わる case folding には
  そもそも対応できない

### 実装方針

汎用ヘルパー `io.github.shomah4a.alle.core.util.StringMatching` に以下を追加する:

- `containsUpperCase(String query)`: smart-case 判定
- `indexOf(String text, String query, int fromIndex, boolean ignoreCase)`: 前方検索
- `lastIndexOf(String text, String query, int fromIndex, boolean ignoreCase)`: 後方検索

`indexOf` / `lastIndexOf` の挙動:

- `ignoreCase = false`: `String.indexOf` / `String.lastIndexOf` に委譲。
- `ignoreCase = true`: `String.regionMatches(true, ...)` を線形に走査。
  - 戻り値は **char offset**。
  - 走査時に `Character.isLowSurrogate(text.charAt(i))` の位置はスキップする
    (上位サロゲートと下位サロゲートの境界を跨ぐマッチを返さない)。
  - 計算量は O(n × m)。エディタ用途のバッファサイズでは許容範囲とする。

`BufferSearcher.searchForward` / `searchBackward` のシグネチャに `boolean caseSensitive` を
追加し、内部で `StringMatching.indexOf` / `StringMatching.lastIndexOf` を呼ぶ。

呼び出し側 (`ISearchSession.searchFrom`、`OccurModel.search`) で
`StringMatching.containsUpperCase(query)` を評価して `caseSensitive` を渡す。

### 後方検索の境界明示

`BufferSearcher.searchBackward(text, query, fromCodePointOffset, caseSensitive)` は
`fromCodePointOffset` 自身を検索範囲に含まない (検索開始位置の直前から後方検索する) ことを
Javadoc に明記する。

`ISearchSession.searchPrevious` が `currentMatch.start()` をそのまま渡す現行ロジックは
この境界に依存しているため、境界を変えると同位置を返し続ける無限ループ相当のバグが起こる。
将来の変更時に責務分担を見失わないよう、Javadoc とテストで守る。

### 設定値の扱い

`case-fold-search` 相当の設定値は **導入しない** (YAGNI)。

将来 `case-fold-search = nil` 相当の挙動を要求するユーザーが出た場合、
`StringMatching.containsUpperCase` の呼び出し箇所に if 分岐を追加するだけで済むよう、
smart-case 判定は呼び出し側 (`ISearchSession` / `OccurModel`) のみに集約する。

## 考慮した代替案

### 案A: 設定値で OFF/ON を切替

ADR 0134 と同じく `Setting<Boolean>` を導入する案。後方互換性を最大化できるが、
ユーザー要求は「emacs 風」つまり smart-case が標準挙動であることを期待している。
YAGNI として今回は採用しない。将来必要になれば追加可能。

### 案B: enum 設定 (OFF / ON / SMART)

ADR 0134 で「将来必要になれば boolean を enum に拡張する余地を残す」と書かれた選択肢。
今回は smart-case 単独で十分なので採用しない。

### 案C: Unicode full case folding 対応 (`Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE`)

正規表現エンジン経由で完全な Unicode case folding を行う案。
ハイライト幅が固定にならず、`SearchResult` の設計を作り直す必要がある。
今回はスコープ外。

## 結果

### メリット

- isearch / occur が emacs 互換の smart-case 挙動になる
- 既存の case sensitive クエリ (大文字を含むクエリ) は挙動が変わらない
- 全て小文字のクエリで大文字混在テキストにヒットするようになり、ファイル名等の検索が楽になる
- 設定値を介さないため、設定の覚え方や切り替え操作が不要

### トレードオフ

- 「全て小文字で常に case sensitive 検索したい」ユーザーには対応しない (設定値を導入していないため)
- case insensitive 経路は `String.indexOf` の HotSpot intrinsic から純 Java の線形走査に劣化する
  - `occur` で 1 行が極端に長いバッファでは速度低下が体感できる可能性がある
  - 通常のソースコード/テキスト用途では許容範囲
- ギリシャ大文字 (Α, Β …) やキリル大文字 (А, Б …) もタイトルケース文字も「大文字」として
  smart-case 判定されるため、これらを含むクエリは case sensitive 扱いになる
  - 結果は通常 case insensitive と同じなので実害はないが、メンタルモデルとはずれる

## 補完 (ADR 0134) との設計差異

| 項目 | 補完 (ADR 0134) | 検索 (本 ADR) |
|---|---|---|
| 切替方式 | 設定値 `completion-ignore-case` (default false) | 常時 smart-case |
| 採用理由 | クエリは打ち切られた途中状態で大文字混入の意図を読みにくい | クエリは意識的に入力された完成形で大文字混入の意図が明確 |
| case folding | `String.regionMatches(true, ...)` 準拠 | 同左 |
| Unicode full case folding | 非対応 | 同左 |

## 関連 ADR

- ADR 0125 (query-replace): query-replace の smart-case 化は本 ADR のスコープに含まない。
  必要になれば別 ADR で扱う。
- ADR 0134 (補完の case-insensitive 化): 本 ADR と同じ `StringMatching` ユーティリティに
  smart-case 用ヘルパーを追加する。設計方針の差異は上記表のとおり。

## 将来課題

- `case-fold-search` 設定値の導入 (smart-case を OFF にしたいユーザーが出た場合)
- `query-replace` の smart-case 化
- Unicode full case folding 対応 (ハイライト幅可変対応も同時に必要)
