# ADR 0139: json-pretty-print コマンド

## ステータス

Accepted

## コンテキスト

Emacs の `json-pretty-print` に相当するコマンドがない。
JSON バッファを開いた際、リージョンを整形したい／バッファ全体を整形したいという操作が発生する。

現状の `JsonMode`（ADR 0028 で導入されたシンタックスハイライトと ADR 0115 で導入された C スタイルインデントを提供）にはフォーマッタが存在しない。

## 決定

### コマンド仕様

- コマンド名: `json-pretty-print`
- キーバインド: なし（M-x 経由で呼び出す）
- 配置: グローバル `CommandRegistry`（`EditorCore` で登録）
- 実装クラス: `io.github.shomah4a.alle.core.command.commands.JsonPrettyPrintCommand`
- 実装: `TransactionalCommand`（Undo 1 単位）

JSON モードに限定せずグローバル登録とする。JSON 整形は他モードのバッファ（スクラッチ、ログ等に貼り付けた JSON など）でも頻繁に必要になるため。

### 対象範囲

- `mark` 未設定 → バッファ全体を対象
- `mark == point`（空リージョン） → バッファ全体を対象
- それ以外 → `[regionStart, regionEnd)` の部分文字列を対象

### 入力パース戦略

Jackson の `JsonParser` を対象文字列で生成し、`ObjectMapper.readTree(parser)` を `MissingNode` が返るまでループ実行して **JSON 値のシーケンス**として解析する。

- 単一 JSON: 1 個 pretty-print
- 複数 JSON（`.jsonl` 相当を含む）: 各値を pretty-print し、`\n` 1 つで連結

`ObjectReader.readValues(text)` は外側の配列 `[1,2,3]` を要素シーケンスに展開してしまい元の配列としての整形ができないため採用しない。

複数値対応により、`.jsonl` バッファでもバッファ全体整形が動作するが、結果は元の 1 行 1 JSON 形式ではなく、各 JSON が展開された整形結果になる。この挙動は仕様として受け入れる。

### インデント

現在のバッファローカル設定を参照する:

- `EditorSettings.INDENT_TABS_MODE = true`: タブ 1 文字 (`\t`) を 1 レベル
- `EditorSettings.INDENT_TABS_MODE = false`: `EditorSettings.INDENT_WIDTH` 個のスペースを 1 レベル

`JsonMode` のデフォルトは `INDENT_WIDTH = 2`, `INDENT_TABS_MODE = false`（クローンされず親設定を継承）。

改行は `\n`（バッファ内部表現に合わせる。ディスク書き出し時に `BufferIO` が `LineEnding` で変換）。

### Jackson 設定

- `DefaultPrettyPrinter` は呼び出し毎に新規生成（内部状態共有を回避）
- `DefaultIndenter` の line separator は `\n` 固定
- オブジェクトフィールドのキーと値の区切りは `: `（コロン前スペースなし、コロン後スペース 1 個）とする。`Separators.createDefaultInstance().withObjectFieldValueSpacing(Separators.Spacing.AFTER)` により設定
- `ObjectMapper` に `DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS = true`, `USE_BIG_INTEGER_FOR_INTS = true` を有効化して数値精度の暗黙損失を回避
- `JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES = false` を設定し、`1.10` のような末尾ゼロを保持
- `StreamReadFeature.STRICT_DUPLICATE_DETECTION = true` を有効化し、重複キー入力（`{"a":1,"a":2}`）をパースエラーとして扱う。整形操作でデータが黙って減ることを防ぐため
- 非 ASCII 文字は Jackson デフォルトで生の Unicode 出力（`ESCAPE_NON_ASCII` は既定 false）

### エラー処理

- ReadOnly バッファ: 事前チェックし `"Buffer is read-only: <buffer name>"` を messageBuffer に表示して終了
- パース失敗 (`JsonProcessingException`): `try-catch` で捕捉し、
  `"JSON parse error: <message>"` を messageBuffer に表示、future は正常完了（バッファ未変更）
- 空バッファ / 空リージョン（実質バッファ全体が空）: 整形せず終了
- 空白のみ入力（JSON 値が 1 個も見つからない）: `"No JSON value found"` を messageBuffer に表示、バッファ未変更で終了

### 整形後の状態

- リージョン整形: 新リージョン先頭に `mark`、末尾に `point` を設定（リージョンを保持）
- バッファ全体整形: `point` を 0 に、`mark` をクリア
- バッファ全体整形時、元テキストが末尾 `\n` で終わっていれば整形後も末尾 `\n` を保持

### リージョン整形時の前後空白吸収

リージョンが `\n  { "a": 1 }\n  ` のように前後空白を含む場合、Jackson は許容してパース成功する。
整形出力にはこれらの前後空白は含まれない。仕様として受け入れる。

バッファ全体整形時のみ末尾 `\n` を復元するのに対し、リージョン整形時は復元しない（前後空白吸収の一環）。
この非対称性は「バッファ全体整形はファイル末尾改行の慣習を尊重する」「リージョン整形はユーザーが指定した範囲を正確に置換する」という意図の違いに基づく。

## 結果

- JSON バッファでの整形操作が可能になる
- `.jsonl` バッファでは元の形式が保持されないため、整形前に必要ならブランチや Undo で退避すること
- 数値表現は BigDecimal/BigInteger 経由で保持される（Jackson の再フォーマット規則には従う）
- 非 ASCII 文字のエスケープ表現は展開される（`â` → 生 UTF-8）
