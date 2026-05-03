# ADR 0134: 補完候補のケース無視マッチ設定

## ステータス

承認

## コンテキスト

ミニバッファ補完（find-file・switch-to-buffer・M-x 等）で前方一致を行うとき、現状はすべて
`String.startsWith` によるケース敏感な比較を行っている。実用上、ファイル名を入力する際に
大文字小文字を意識せずに補完したい場面は多い。

Emacs では `completion-ignore-case` 変数で補完のケース無視を切り替えることができ、本リポジトリも
これに倣った仕組みを導入する。

ただし、Linux のファイルシステムはケース敏感であり、補完候補とユーザー入力でケースが食い違う
場合の確定処理（`PathResolver.expandTilde` 後の実ファイルアクセスなど）にも配慮が必要。

## 決定

### 設定値

- `EditorSettings.COMPLETION_IGNORE_CASE`: `Setting<Boolean>`
- key: `completion-ignore-case` (Emacs 互換命名)
- デフォルト値: `false` （既存ユーザーの動作変更を避ける）
- グローバルレベルでのみ参照する（バッファローカルでは扱わない）

### マッチング方針

- ケース比較は `String.regionMatches(true, 0, prefix, 0, prefix.length())` を採用
  - `Character.toLowerCase(char)` ベースとし、Locale 非依存
  - Turkish-I (`İ` ↔ `i`) のような Unicode case folding は対象外
- 共有ヘルパー `io.github.shomah4a.alle.core.util.StringMatching.startsWithIgnoreCase(str, prefix)` に集約
  - 補完専用の名前空間ではなく汎用的な `core.util` パッケージに置くことで、`input` パッケージと
    `command` パッケージの双方から依存方向を破らずに参照できるようにする

### 影響範囲

以下 5 箇所で、`boolean ignoreCase` 引数を導入してケース無視マッチに対応する。

1. `FilePathCompleter` (find-file, save-buffer 等)
2. `BufferNameCompleter` (switch-to-buffer, kill-buffer)
3. `CommandNameCompleter` + `CommandResolver.completeFqcn` (M-x)
4. `FrameLayoutNameCompleter` (フレームレイアウト)
5. `KillBufferCommand` の `KILL_CONFIRM_COMPLETER` (yes/no/save and kill のリテラル候補)

リテラル候補補完も対象に含めるのは、Emacs `completion-ignore-case` の挙動と一貫させるため。
`Y` 入力で `yes` 候補にマッチさせたい場合などが該当する。

### 最長共通プレフィックス

`CompletionResult.longestCommonPrefix(strings, ignoreCase)` を新規追加する。

- 比較は `String.regionMatches(true, ...)` ベース
- 文字列の進行は **codePoint 単位** で行う（`String.offsetByCodePoints`、`String.codePointAt`）
- 戻り値は **先頭候補から `substring`** で取り出す（codePoint 境界に揃った文字インデックス）
- サロゲートペア境界の中間で切れて壊れた文字列を返さない
- 候補のケースが混在する場合でも、戻り値は先頭候補のケースに揃う（順序依存）

`resolveDetailed(input, candidates, ignoreCase)` オーバーロードも追加。

### 補完候補のケースで入力を上書きする挙動

ignore-case が有効なとき、補完で候補側のケースに揃える（Emacs 互換）。

- 例: 入力 `src` → 候補 `Source/` → ミニバッファ表示は `Source/`
- 例: 入力 `source/` → 候補 `Source/` → partial 確定保留時に `Source/` に書き換え

これにより、Linux のケース敏感な FS でもケース不一致による確定失敗を防ぐ。

### MinibufferInputPrompter の修正

- コンストラクタに `BooleanSupplier ignoreCaseSupplier` を追加
- `MinibufferConfirmCommand`: partial 一致判定を `ignoreCase ? equalsIgnoreCase : equals` で行い、
  ignore-case で値が異なる場合は `replaceUserInput(promptLength, candidates.get(0).value())` で
  候補側に書き換えてから保留する
- `MinibufferCompleteCommand`: `CompletionResult.resolveDetailed` 呼び出し時に ignoreCase を渡す
- 複数候補が partial 一致する場合の RET 確定挙動は変更しない（現状維持）

### FilePathInputPrompter の修正

- コンストラクタに `BooleanSupplier ignoreCaseSupplier` を追加
- `prompt()` 内で `ignoreCaseSupplier.getAsBoolean()` を解決して `FilePathCompleter` に渡す
- `ShadowAwareCompleter` は delegate 委譲のみで ignoreCase の解釈は不要

### 配線方針

入力層 (`FilePathInputPrompter` / `MinibufferInputPrompter`) を `SettingsRegistry` から
独立させるため、`BooleanSupplier` 経由で値を渡す。

- `EditorCore` で生成する際に
  `() -> settingsRegistry.getGlobal(EditorSettings.COMPLETION_IGNORE_CASE).orElse(false)` を渡す
- 各 Command (`SwitchBufferCommand` など) では
  `context.settingsRegistry().getGlobal(EditorSettings.COMPLETION_IGNORE_CASE).orElse(false)`
  を Completer コンストラクタに渡す
- `Main.java` で `settingsRegistry.register(EditorSettings.COMPLETION_IGNORE_CASE)` を行う

`BooleanSupplier` 採用により以下の効果がある:
- 入力層が `SettingsRegistry` に直接依存しない（責務分離）
- 設定を実行時に変更しても次回 prompt から即座に反映される
- テストでは `() -> true` / `() -> false` で簡単に切り替えられる

## 考慮した代替案

### 案A: 常時 case-insensitive

実装は最も単純だが、既存ユーザーの動作を変えてしまう。設定で切り替え可能な案 B を採用。

### 案C: smart-case（入力に大文字を含むときのみ case-sensitive）

vim や ripgrep の挙動。学習コストはあるが直感的。今回は採用しない。
将来必要になれば、`completion-ignore-case` 設定値の選択肢を boolean から enum
（OFF/ON/SMART）に拡張する余地を残す。

### 配線案: SettingsRegistry をコンストラクタ注入

`FilePathInputPrompter` / `MinibufferInputPrompter` のコンストラクタに直接 `SettingsRegistry`
を渡す案。設定の取得は素直になるが、入力層に設定層への依存が生まれる。
責務分離の観点で `BooleanSupplier` 案を採用した。

### 配線案: prompt メソッドの引数で boolean を渡す

`prompt(...)` の各オーバーロードに `boolean ignoreCase` を追加し、呼び出し側 Command で解決
する案。呼び出し側のヌケモレリスクが大きく、署名変更が広範に及ぶため不採用。

## 結果

### メリット

- ユーザーが設定で補完のケース無視を ON/OFF できる
- 既存動作はデフォルト OFF のため変わらない
- Linux のケース敏感な FS でも、補完が候補側のケースに揃うため確定失敗を防げる
- 入力層を設定層から独立させられる

### トレードオフ

- ignore-case ON 時は、ユーザーが入力した小文字が補完で大文字に置換されるなど、表示が
  入力意図と変わる場面が出る（Emacs と同じ挙動）
- リテラル補完候補（yes/no 等）も影響を受けるため、入力 `Y` で `yes` 候補にマッチする
  ような挙動の変化が生じる

## 将来課題

- 設定値をスクリプト（init.py）から変更する API は現状未整備。別タスクとして整備する
  （`docs/tasks.md` 参照）。
- smart-case 対応はスコープ外。今回採用した boolean 設計から enum 設計への拡張で対応可能。

## 関連 ADR

- ADR 0128 (file-open-service): find-file の入力解決ロジック。本変更で補完段階のみケース無視
  になり、確定後の実ファイルアクセスは ADR 0128 の責務をそのまま継承する。
