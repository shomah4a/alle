# ADR 0125: 対話型置換（query-replace / query-replace-regexp）

## ステータス

承認

## コンテキスト

エディタに検索機能（ADR 0089 i-search）はあるが、マッチを置換する機能がない。
Emacs の `query-replace` (M-%) / `query-replace-regexp` (C-M-%) に相当する対話型置換コマンドを導入する。

非対話版の `replace-string` / `replace-regexp` は今回のスコープ外とする（ユーザー指示により対話版のみ）。

## 決定

### コマンド

- `query-replace` — リテラル文字列置換
- `query-replace-regexp` — 正規表現置換

実装は対話版のみだが、Emacs 互換として以下の別名も登録し、同じ対話型置換コマンドに割り当てる。履歴は対応するコマンド同士で共有する:

- `replace-string` → `query-replace` と同じ実装を共有
- `replace-regexp` → `query-replace-regexp` と同じ実装を共有

これは Emacs における `replace-string` / `replace-regexp`（本来は非対話）を対話型として提供する方針であり、純粋な Emacs 互換からは逸脱する。Emacs 本来の非対話版が必要になった時点で別コマンドとして切り出す（TODO）。

### 起動フロー

1. ミニバッファで FROM をプロンプト
2. ミニバッファで TO をプロンプト
3. 置換範囲を決定:
   - リージョン（mark）が有効ならその範囲内のみ
   - それ以外は point からバッファ末尾まで
4. 最初のマッチを検索する
5. マッチがあれば `QUERY_REPLACE_MATCH` Face でハイライトし overriding keymap を設定
6. マッチがなければ "Replaced 0 occurrences" を表示して終了

### 対話キー（v1 最小セット）

- `y` / `SPC`: 現在のマッチを置換して次のマッチへ
- `n` / `DEL`: 置換せず次のマッチへ
- `!`: 残り全マッチを無確認で置換
- `C-g`: キャンセル（ここまでの置換結果は残す）
- 上記以外のキー: `overriding keymap` の onUnboundKeyExit による確定終了。キー自体は通常キーマップ解決にフォールスルーする（ADR 0089 i-search と同様の挙動）

Emacs にある `.`（置換して終了）/ `^`（戻る）/ `q`（明示中止）/ `E`（edit replacement）などは v1 では未対応とする。TODO として `docs/tasks.md` に記録する。

### 終了処理

すべての終了パス（確定/キャンセル/非対象キー）で以下を行う:

- ハイライト除去
- overriding keymap クリア
- point は `query-replace` 起動時の位置に復元する（Emacs 挙動）
- リージョンを使った場合は clearMark
- エコーエリアに `Replaced N occurrences` を表示

### 逐次再検索方式

事前に全マッチを列挙する方式は採らない。各置換/スキップの後で現在のバッファ状態に対して再検索する:

- 検索開始位置は直前のマッチ終端（置換ならその長さを加味）
- 置換によって range（リージョン時）の末尾がずれるため、`rangeEnd += (to.length - matched.length)` を維持する
- regex で空マッチが発生した場合は開始位置を 1 コードポイント進めて無限ループを防ぐ

この方針は、overriding keymap 中に想定外のバッファ変更が入ってもマッチ不整合を起こしにくい（事前列挙方式は delta 補正に依存するため脆い）。

### 置換位置（リテラル）

`BufferSearcher` と同じく `String#indexOf` ベース。char offset → コードポイント offset の変換は `BufferSearcher` に既存のユーティリティがあるのでそちらを流用する（必要ならパッケージ可視性を調整）。

### Pattern コンパイル

`java.util.regex.Pattern.compile(from)` を使う。flag はデフォルト。コンパイル失敗時は `PatternSyntaxException` を捕捉し、エコーエリアと `*Warnings*` にエラーを出して起動をキャンセルする。

### 範囲内マッチの境界

- リテラル: マッチ全体が `[rangeStart, rangeEnd]` に収まる場合のみ採用（`end <= rangeEnd`）
- regex: `Matcher#region(startChar, endChar)` を使う。`useAnchoringBounds(false)`, `useTransparentBounds(false)` を設定し、`^`/`$` を範囲先頭/末尾に引き寄せない（Java デフォルト挙動=バッファ先頭/行末/バッファ末尾を維持）

### TO テンプレートの展開（regex）

Emacs 互換で以下のエスケープを独自に解釈する。`Matcher#appendReplacement` は `$` を特殊扱いするため使わず、コードポイント列を走査する独自展開を行う。

- `\&` → マッチ全体
- `\0` → マッチ全体
- `\1`..`\9` → 対応するキャプチャグループ（存在しなければ空文字列）
- `\\` → `\` 一文字
- 他の `\X` → リテラル `X`（Emacs 互換の寛容解釈）
- `$` は非特殊

### undo 単位

1 対話コマンド（y / n / !）あたり 1 undo 単位とする。
`CommandLoop.handleEntry` が全コマンドの `execute` を `UndoManager#withTransaction` で包むため、セッション側は個別にトランザクションを張らない。`!` の一括置換ループは 1 コマンド内で動作するため、まとめて 1 undo 単位になる。

Emacs では query-replace セッション全体を 1 undo 単位にするが、現行 `UndoManager` のトランザクション API が Runnable ベースでネストを許さない開始/終了型 API を持たないため、セッション全体の統合 undo は見送る。`UndoManager` に `UndoTransaction beginTransaction()` 型 API を足した時点で統合する（TODO）。

### キーバインド

- `M-%` → `query-replace`
- `C-M-%` → `query-replace-regexp`

既存キーマップ（ADR 0089 ほか）と衝突しないことを確認済み。TUI で `C-M-%` が取得できない環境でも `M-x query-replace-regexp` で呼び出せる。

### FaceName

- `QUERY_REPLACE_MATCH` を新設する（`ISEARCH_MATCH` と分離）
- `DefaultFaceTheme` に `ISEARCH_MATCH` と同等の見た目（黄色背景/黒文字/太字）で登録

### 実装構造

- `ReplaceEngine` — 置換検索（リテラル/regex）と TO テンプレート展開
- `ReplaceMatch` — sealed interface: `Literal` / `Regex`
- `QueryReplaceSession` — セッション状態管理（i-search と同じパターン）
- `QueryReplaceCommand` / `QueryReplaceRegexpCommand` — 起動コマンド
- `QueryReplaceYesCommand` / `QueryReplaceNoCommand` / `QueryReplaceAllCommand` / `QueryReplaceCancelCommand` — overriding keymap 配下のアクション

## 影響

- `search` パッケージにクラスが追加される。既存 i-search には影響なし。
- `FaceName` / `DefaultFaceTheme` に `QUERY_REPLACE_MATCH` が追加される。
- `EditorCore` に 2 コマンドと 4 履歴が追加される。キーバインド追加あり（既存未使用）。
- 1 置換 1 undo 単位になるため、Emacs と厳密に同じ undo 粒度ではない。
