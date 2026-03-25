"""Python モード固有のコマンド定義。

言語非依存のコマンド（indent-dedent-backspace, comment-dwim, indent-region,
dedent-region, comment-region）はコア側（Java）に実装されている。
"""

from __future__ import annotations

import re

import alle
from alle.command import command

_INDENT_UNIT = 4

# コロンで終わる行のパターン（コメント・文字列内のコロンは除外）
_COLON_END = re.compile(r":\s*(?:#.*)?$")

# dedentキーワードで終わる行のパターン
_DEDENT_END = re.compile(r"\b(?:return|pass|break|continue|raise)\b\s*(?:#.*)?$")

# 行頭の空白を取得するパターン
_LEADING_WHITESPACE = re.compile(r"^(\s*)")

# TABインデント循環のための状態
_last_indent_line: int = -1
_last_indent_cycle: int = 0


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


@command("python-indent-line")
def indent_line():
    """カーソル行のインデントをコンテキストに応じて調整する。

    TAB を連続で押すとインデントレベルを循環する。
    """
    global _last_indent_line, _last_indent_cycle

    win = alle.active_window()
    buf = win.buffer()
    point = win.point()
    line_index = buf.line_index_for_offset(point)

    if line_index > 0:
        prev_indent_len = len(_get_indent(buf.line_text(line_index - 1)))
    else:
        prev_indent_len = 0

    current_indent_len = len(_get_indent(buf.line_text(line_index)))

    candidates = []
    seen = set()
    for level in [prev_indent_len, prev_indent_len + _INDENT_UNIT,
                  max(prev_indent_len - _INDENT_UNIT, 0), 0]:
        if level not in seen:
            candidates.append(level)
            seen.add(level)

    if _last_indent_line == line_index and _last_indent_cycle < len(candidates):
        cycle_index = _last_indent_cycle
    else:
        if current_indent_len in candidates:
            idx = candidates.index(current_indent_len)
            cycle_index = (idx + 1) % len(candidates)
        else:
            cycle_index = 0

    new_indent = " " * candidates[cycle_index]
    _set_line_indent(win, buf, line_index, new_indent)
    _last_indent_line = line_index
    _last_indent_cycle = (cycle_index + 1) % len(candidates)


@command("python-newline-and-indent")
def newline_and_indent():
    """改行を挿入し、前行のインデントを継承する。

    前行が ``:`` で終わる場合はインデントを1レベル増加する。
    前行が ``return``/``pass``/``break``/``continue``/``raise`` で終わる場合は
    インデントを1レベル減少する。
    """
    win = alle.active_window()
    buf = win.buffer()
    point = win.point()

    line_index = buf.line_index_for_offset(point)
    line_start = buf.line_start_offset(line_index)
    text_before_cursor = buf.substring(line_start, point)

    indent = _get_indent(text_before_cursor)

    if _COLON_END.search(text_before_cursor):
        indent += " " * _INDENT_UNIT
    elif _DEDENT_END.search(text_before_cursor):
        if len(indent) >= _INDENT_UNIT:
            indent = indent[:-_INDENT_UNIT]

    win.insert("\n" + indent)
