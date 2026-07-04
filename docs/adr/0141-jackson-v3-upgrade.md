# ADR 0141: Jackson 3 (tools.jackson) へのアップグレード

## ステータス

Accepted

## コンテキスト

JSON 処理に jackson-databind 2.22.0 (`com.fasterxml.jackson.core:jackson-databind`) を使用している。
Jackson 2 系は 2.22 以降メジャー開発が 3 系へ移行しており、3.1 系が LTS として提供されている。

利用箇所は `alle-core` の 2 ファイルのみ:

- `ServerProtocol` (ADR 0129): JSON Lines プロトコルの encode/parse。Tree Model のみ
- `JsonPrettyPrintCommand` (ADR 0139): カスタム PrettyPrinter による整形。Tree Model + streaming parser

Jackson アノテーションおよび POJO データバインディングは使用していない。

Jackson 3 の主要な破壊的変更 (今回影響するもの):

- Maven 座標: `com.fasterxml.jackson.core:jackson-databind` → `tools.jackson.core:jackson-databind`
- Java パッケージ: `com.fasterxml.jackson.*` → `tools.jackson.*`
- 例外体系: checked (`JsonProcessingException` / `IOException`) → unchecked (`JacksonException` 系)
- `DeserializationFeature.FAIL_ON_TRAILING_TOKENS` のデフォルトが false → true
- streaming factory と mapper の分離 (`ObjectCodec` 廃止、`ObjectReadContext` / `ObjectWriteContext` 導入)

## 決定

### バージョン

- **3.1.4** (3.1 系 LTS の最新パッチ、Maven Central で実在確認済み 2026-07-04) に固定する
- 3.2.0 (最新、非 LTS) は採用しない。LTS のサポート期間を優先するユーザー判断による

### ServerProtocol の後続トークン付き行の扱い (仕様変更)

`FAIL_ON_TRAILING_TOKENS` の v3 デフォルト (有効) をそのまま採用し、**厳格化を仕様とする**。

- v2 まで: `{"type":"finished"} garbage` のような行は先頭の JSON 値をパースして受理
- v3 から: 上記のような行は不正行として `Optional.empty()` を返す

JSON Lines プロトコルとして 1 行 1 JSON 値の厳格な形式のみを受理する。
現行クライアントは自前実装のみであり、後続トークン付き行を送信する実装は存在しないため実害はない。
この挙動変化は characterization テストで明示的に固定する (移行前に v2 挙動を固定 → 移行コミットで意図的に期待値を更新)。

### JsonPrettyPrintCommand の複数 JSON 値対応の維持

ADR 0139 の「JSON 値のシーケンスとして解析する」仕様は維持する。
`FAIL_ON_TRAILING_TOKENS` のデフォルト有効化がこのループと干渉する場合、`JsonPrettyPrintCommand` 専用 mapper でのみ明示 disable する。
`ServerProtocol` の厳格化とは mapper が別インスタンスであるため矛盾しない。

実装時の検証結果: `FAIL_ON_TRAILING_TOKENS` は readTree ループと**干渉した**。
`mapper.createParser(source)` で生成した parser に対する `mapper.readTree(parser)` ループでも、
1 値目の読み取り完了時点で後続の `START_OBJECT` トークンを検出して
`MismatchedInputException` (Trailing token found after value) を送出する。
このため `JsonPrettyPrintCommand` 専用 mapper で `FAIL_ON_TRAILING_TOKENS` を明示 disable した。

その他の実装時検証結果:

- EOF・空入力時の `mapper.readTree(parser)` は null を返す (missing node ではない)。
  ループの `node == null || node.isMissingNode()` 判定により「No JSON value found」経路は維持される
- parser 生成は `ObjectMapper.createParser(String)` を使用。
  `StreamReadFeature.STRICT_DUPLICATE_DETECTION` が parser に伝播し、
  重複キーで `StreamReadException` (Duplicate Object property) が送出されることを確認した
- 例外 catch は両ファイルとも `tools.jackson.core.JacksonException` とした。
  パース失敗は `StreamReadException` (不正 JSON・重複キー) と `MismatchedInputException` (後続トークン)
  の 2 系統に分かれ、さらに `StreamConstraintsException` 等の直接サブタイプも存在するため、
  これらを過不足なく覆う最小の共通型は基底の `JacksonException` (v2 の `JsonProcessingException` に相当) となる
- `ObjectMapper.writer(PrettyPrinter)` は v3 で削除されたため `writer().with(printer)` に変更
- `Separators.withObjectFieldValueSpacing` は v3 で `withObjectNameValueSpacing` にリネーム
- `JsonNode.isTextual()` / `asText()` は v3 で deprecated となったため `isString()` / `asString()` に変更

### parser 生成

parser は streaming factory から直接生成せず、mapper 経由の API で生成する。
factory 直接生成では mapper builder で設定した `StreamReadFeature.STRICT_DUPLICATE_DETECTION` が parser に伝播しないため。
既存テスト「重複キーはパースエラーになりバッファは変更されない」を検証の基準とする。

### 例外ハンドリング

- `JsonProcessingException` / `IOException` の catch は Jackson 3 の unchecked 例外 (`JacksonException` 系) の catch に置き換える
- unchecked 化により catch を削除してもコンパイルが通るため、Jackson API 呼び出し全箇所の例外経路をレビューし、パース失敗時の既存挙動 (`Optional.empty()` 返却 / メッセージ表示) を維持する
- `RuntimeException` の広域 catch による回避は他のバグを隠蔽するため行わない

### feature フラグの明示指定の維持

現在明示指定している feature フラグ (`USE_BIG_DECIMAL_FOR_FLOATS`, `USE_BIG_INTEGER_FOR_INTS`, `STRICT_DUPLICATE_DETECTION`, `STRIP_TRAILING_BIGDECIMAL_ZEROES = false`) は、v3 のデフォルトと一致していても削除しない。
将来のマイナーバージョンでのデフォルト変更に対する明示的ピン留めとして維持する。

### 互換性確認の基準

既存テスト (`ServerProtocolTest`, `JsonPrettyPrintCommandTest`) の通過を互換性確認の基準とする。
移行に伴うテストの期待値・アサーションの変更は禁止する (import の追随と、上記 characterization テストの意図的更新を除く)。

## 帰結

- JSON 出力形式・整形結果は v2 と同一を維持する (テストで固定)
- `ServerProtocol.parseJson` の受理範囲が狭くなる (上記の通り仕様として採用)
- 依存座標・パッケージが `tools.jackson` 系となり、Jackson 2 系とはクラスパス上共存可能な形になる

## 参考

- 計画文書: `.claude/tmp/2026-07-04_jackson-v3-upgrade.md`
- 防衛的計画評価: `.claude/tmp/2026-07-04_jackson-v3-upgrade-defensive-review.md`
- https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md
- https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-2
