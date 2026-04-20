# ADR 0124: shebang によるメジャーモード推測

## ステータス

承認

## コンテキスト

`AutoModeMap` は現状「ファイル名完全一致 > 拡張子マッチ > デフォルトモード」でメジャーモードを解決する（ADR 0025, ADR 0121）。
拡張子を持たないスクリプトファイル（例: `myscript` の先頭に `#!/usr/bin/env python3`）ではデフォルトモードが適用され、ユーザーが期待するモードにならない。

Emacs の `interpreter-mode-alist` に相当する仕組みとして、shebang 行のインタプリタ名からモードを推測する機能を導入する。
併せて、モード作成時に対応するインタプリタコマンドを明示的に登録できる API を追加する。

## 決定

### AutoModeMap への shebang マッピング追加

- `registerShebang(String command, Supplier<MajorMode> factory)` を追加し、インタプリタコマンド名（basename）とファクトリを紐付ける。
- `resolve` を2引数版 `resolve(String fileName, Supplier<String> firstLineSupplier)` としてオーバーロードする。既存の1引数版 `resolve(String fileName)` は shebang 解決を行わない実装として残す（責務を明確に分離するため `() -> ""` 委譲ではなく別実装）。
- 先頭行 Supplier は shebang 判定が必要になった場合にのみ呼ばれる遅延評価。

### 解決順位

1. ファイル名完全一致
2. 拡張子マッチ
3. shebang マッチ
4. デフォルトモード

Emacs の `interpreter-mode-alist` に準拠する（拡張子ベースの `auto-mode-alist` が優先）。明示的な拡張子が付いている場合、ユーザーはその拡張子に対応するモードを期待するため、shebang の推測より優先する方が予測可能性が高い。

### shebang パース仕様

- 先頭行が `#!` で始まらなければマッチしない（即 null）。
- 以降の文字列を空白で分割し、最初のトークンのパス basename を取得する。
- basename が `env` の場合は、続くトークンを順に見て「`-` で始まる」「`=` を含む」ものをスキップし、最初に該当しないトークンの basename を対象とする。
  - `#!/usr/bin/env -S python3` → `python3`
  - `#!/usr/bin/env VAR=value python3` → `python3`
- basename が完全一致で登録されていれば対応ファクトリを返す。
- 不正な文字や空先頭行では null を返し、例外は上位に伝播させない。

### バッファ先頭行の取得

- 既存の `BufferFacade.lineText(0)` を利用する（`lineCount() == 0` のケースは存在しないが、length が0のときは空文字列が返る）。
- 空バッファ（ファイル未存在で新規作成されたケースなど）では shebang マッチはせず、拡張子/デフォルトの既存経路で処理される。
- 先頭行が極端に長いケースは `#!` 先頭チェックで早期に落ちるため実害はない。

### 呼び出し側の統一

- `FindFileCommand` と `TreeDiredFindFileOrToggleCommand` の両方で同じ解決ロジック（`resolve(fileName, firstLineSupplier)`）を使う。
- 呼び出し箇所が2箇所のみのため、`BufferFacade.lineText(0)` を返す小さなラムダを各所で直接書き、共通ヘルパの導入は見送る。将来呼び出し箇所が増えた場合はヘルパ化を検討する。

### EditorCore への shebang 登録

既存のモードに対して以下を登録する:

- `ShellScriptMode`: `sh`, `bash`
- `JavaScriptMode`: `node`, `nodejs`

### EditorFacade / Python API

- `EditorFacade.registerAutoModeShebang(String command, String modeName)` を追加。既存の `registerAutoMode` と対になる API。
- Python API `alle.register_major_mode` に keyword-only な `shebangs` 引数を追加し、指定された各 shebang コマンドについて `registerAutoModeShebang` を呼ぶ。
- `alle/modes/__init__.py` の PythonMode 登録に `shebangs=["python", "python2", "python3"]` を追加。

## 影響

- `AutoModeMap` にファクトリマップと shebang パース処理が追加される（既存の拡張子マッチ・ファイル名マッチは変更なし）。
- `FindFileCommand` と `TreeDiredFindFileOrToggleCommand` が新 resolve 経路を使う。
- `EditorCore` でシェル/JS モードに shebang が紐付く。拡張子ベース解決が勝つため既存の `.sh` / `.js` ファイルの挙動は変わらない。
- alle-script の Python API に `shebangs` 引数が増える（keyword-only のため既存呼び出しに影響なし）。
- 組み込み PythonMode は拡張子なしかつ python shebang を持つファイルで自動的に python-mode で開かれるようになる。
