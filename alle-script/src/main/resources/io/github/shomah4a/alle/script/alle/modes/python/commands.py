"""Python モード固有のコマンド定義。

言語非依存のコマンド（indent-dedent-backspace, comment-dwim, indent-region,
dedent-region, comment-region）はコア側（Java）に実装されている。
"""

from __future__ import annotations

import re
from typing import TYPE_CHECKING, Any

import java

from alle.command import CommandBase

if TYPE_CHECKING:
    from alle.context import CommandContext

_INDENT_UNIT = 4

# コロンで終わる行のパターン（コメント・文字列内のコロンは除外）
_COLON_END = re.compile(r":\s*(?:#.*)?$")

# dedentキーワードで終わる行のパターン
_DEDENT_END = re.compile(r"\b(?:return|pass|break|continue|raise)\b\s*(?:#.*)?$")

# 行頭の空白を取得するパターン
_LEADING_WHITESPACE = re.compile(r"^(\s*)")

_Lists: Any = java.type('org.eclipse.collections.api.factory.Lists')


def _get_indent(text: str) -> str:
    """行テキストから先頭の空白を取得する。"""
    match = _LEADING_WHITESPACE.match(text)
    return match.group(1) if match else ""


def _set_line_indent(win, buf, line_index: int, new_indent: str) -> None:
    """指定行のインデントを変更する。"""
    line_start = buf.line_start_offset(line_index)
    line_text = buf.line_text(line_index)
    old_indent = _get_indent(line_text)
    if old_indent == new_indent:
        return
    old_len = len(old_indent)
    buf.delete_at(line_start, old_len)
    buf.insert_at(line_start, new_indent)
    point = win.point()
    if point < line_start + old_len:
        win.goto_char(line_start + len(new_indent))
    else:
        win.goto_char(point - old_len + len(new_indent))


def _buffer_lines(buf) -> Any:
    """バッファの全行をJava ListIterable として返す。"""
    line_count = buf.line_count()
    lines = _Lists.mutable.withInitialCapacity(line_count)
    for i in range(line_count):
        lines.add(buf.line_text(i))
    return lines


def _get_bracket_indent(syntax_analyzer, buf, line_index: int, column: int) -> int | None:
    """構文解析で括弧内にいるかを判定し、括弧内ならインデントカラムを返す。

    括弧内でない場合や構文解析器が利用不可の場合は None を返す。
    """
    if syntax_analyzer is None:
        return None
    lines = _buffer_lines(buf)
    tree = syntax_analyzer.analyze(lines)
    bracket = tree.enclosingBracket(line_index, column)
    if bracket.isEmpty():
        return None
    node = bracket.get()
    bracket_start_line = node.startLine()
    bracket_start_col = node.startColumn()
    bracket_end_line = node.endLine()
    # カーソルが括弧の開始行と同じ行にいる場合はNone（まだ改行していない）
    if line_index == bracket_start_line:
        return None
    # カーソルが括弧の終了行より後にいる場合はNone
    if line_index > bracket_end_line:
        return None
    # 括弧の開始位置の次のカラムをインデントとして返す
    return bracket_start_col + 1


def _build_indent_candidates(prev_indent_len: int, bracket_indent: int | None) -> list[int]:
    """インデントサイクルの候補リストを生成する。"""
    candidates: list[int] = []
    seen: set[int] = set()

    # 括弧インデントがある場合は最優先候補
    if bracket_indent is not None:
        candidates.append(bracket_indent)
        seen.add(bracket_indent)

    for level in [prev_indent_len,
                  max(prev_indent_len - _INDENT_UNIT, 0), 0]:
        if level not in seen:
            candidates.append(level)
            seen.add(level)
    return candidates


class PythonIndentState:
    """Python モードのインデントサイクル状態。

    PythonMode のインスタンスメンバとして保持される。
    """

    def __init__(self, syntax_analyzer: Any | None) -> None:
        self.syntax_analyzer = syntax_analyzer
        self.last_indent_line: int = -1
        self.last_indent_cycle: int = 0

    def cycle_indent(self, win, buf, direction: int) -> None:
        """インデントサイクルの共通ロジック。

        :param win: ウィンドウファサード
        :param buf: バッファファサード
        :param direction: 循環方向。+1 で順方向、-1 で逆方向。
        """
        point = win.point()
        line_index = buf.line_index_for_offset(point)

        if line_index > 0:
            prev_indent_len = len(_get_indent(buf.line_text(line_index - 1)))
        else:
            prev_indent_len = 0

        current_indent_len = len(_get_indent(buf.line_text(line_index)))

        # 構文解析によるカッコ内インデント検知
        bracket_indent = _get_bracket_indent(self.syntax_analyzer, buf, line_index, 0)

        candidates = _build_indent_candidates(prev_indent_len, bracket_indent)

        if self.last_indent_line == line_index and 0 <= self.last_indent_cycle < len(candidates):
            # 連続操作: 現在位置から direction 方向に進む
            cycle_index = (self.last_indent_cycle + direction) % len(candidates)
        else:
            # 新規操作: 現在のインデントに対応する位置から direction 方向に進む
            if current_indent_len in candidates:
                idx = candidates.index(current_indent_len)
                cycle_index = (idx + direction) % len(candidates)
            else:
                cycle_index = 0 if direction == 1 else len(candidates) - 1

        new_indent = " " * candidates[cycle_index]
        _set_line_indent(win, buf, line_index, new_indent)
        self.last_indent_line = line_index
        self.last_indent_cycle = cycle_index

    def newline_and_indent(self, win, buf) -> None:
        """改行を挿入し、前行のインデントを継承する。

        :param win: ウィンドウファサード
        :param buf: バッファファサード
        """
        point = win.point()

        line_index = buf.line_index_for_offset(point)
        line_start = buf.line_start_offset(line_index)
        text_before_cursor = buf.substring(line_start, point)

        indent = _get_indent(text_before_cursor)

        # 構文解析によるカッコ内インデント検知
        col = point - line_start
        bracket_indent = _get_bracket_indent(self.syntax_analyzer, buf, line_index, col)

        if bracket_indent is not None:
            indent = " " * bracket_indent
        elif _COLON_END.search(text_before_cursor):
            indent += " " * _INDENT_UNIT
        elif _DEDENT_END.search(text_before_cursor):
            if len(indent) >= _INDENT_UNIT:
                indent = indent[:-_INDENT_UNIT]

        win.insert("\n" + indent)


def create_indent_commands(state: PythonIndentState) -> tuple[CommandBase, CommandBase, CommandBase]:
    """PythonIndentState をキャプチャしたコマンドを生成する。

    :param state: インデント状態
    :return: (indent_line, dedent_line, newline_and_indent) のタプル
    """

    class IndentLine(CommandBase):
        def name(self) -> str:
            return "python-indent-line"
        def run(self, ctx: CommandContext) -> None:
            win = ctx.active_window()
            buf = win.buffer()
            state.cycle_indent(win, buf, 1)

    class DedentLine(CommandBase):
        def name(self) -> str:
            return "python-dedent-line"
        def run(self, ctx: CommandContext) -> None:
            win = ctx.active_window()
            buf = win.buffer()
            state.cycle_indent(win, buf, -1)

    class NewlineAndIndent(CommandBase):
        def name(self) -> str:
            return "python-newline-and-indent"
        def run(self, ctx: CommandContext) -> None:
            win = ctx.active_window()
            buf = win.buffer()
            state.newline_and_indent(win, buf)

    return IndentLine(), DedentLine(), NewlineAndIndent()
