# ADR 0112: Pythonモード インデントサイクル候補の修正

## ステータス

採用

## コンテキスト

Pythonモードの `cycle_indent`（Tabキーによるインデントサイクル）で、前行がコロン(`:`)や開き括弧で終わる場合にインデント増加候補（`prev_indent_len + INDENT_UNIT`）が生成されない。

例えば `def functionname():` の次行でTabを押しても、候補が `[0]` のみとなり4スペースインデントに到達できない。`class`, `if`, `for`, `while` 等のコロン終端行すべてで同じ問題がある。

Java版の `CStyleIndentState.buildIndentCandidates` では `prevLineEndsWithOpenBracket` フラグにより開き括弧終端行の場合にインデント増加候補を追加しているが、Python版の `_build_indent_candidates` にはこのロジックが欠けている。

なお `newline_and_indent`（Enterキーによるオートインデント）は正しく動作している。

## 決定

`_build_indent_candidates` に前行がインデント増加を要するかのフラグを追加し、True の場合に `prev_indent_len + _INDENT_UNIT` を候補に含める。

前行の判定は正規表現（`_COLON_END`, `_OPEN_BRACKET_END`）でまずマッチさせ、構文解析器（tree-sitter）が利用可能な場合はマッチ位置がコメントや文字列リテラル内でないかをASTで検証する。これによりコメント末尾のコロン（`# Note:` 等）での誤検出を防ぐ。

Python版ではコロン終端（`_COLON_END`）と開き括弧終端（`_OPEN_BRACKET_END`）の両方をインデント増加トリガーとする。これはJava版（開き括弧のみ）より広い範囲だが、Pythonの構文上コロン終端がブロック開始を意味するため妥当である。

## 影響

- 変更対象: `alle-script/.../modes/python/commands.py` の `_build_indent_candidates` と `cycle_indent`
- 新規追加: `_is_inside_comment_or_string`, `_prev_line_needs_indent` ヘルパー関数
- `newline_and_indent` は変更なし
- Java版 `CStyleIndentState` は変更なし
