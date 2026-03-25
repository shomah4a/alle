"""Python モード固有のコマンド定義。"""

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
    # 古いインデントを削除して新しいインデントを挿入
    old_len = len(old_indent)
    buf.delete_at(line_start, old_len)
    buf.insert_at(line_start, new_indent)
    # カーソルがインデント領域内にあった場合、新しいインデント末尾に移動
    point = win.point()
    if point < line_start + old_len:
        win.goto_char(line_start + len(new_indent))
    else:
        win.goto_char(point - old_len + len(new_indent))


@command("python-indent-line")
def indent_line():
    """カーソル行のインデントをコンテキストに応じて調整する。

    TAB を連続で押すとインデントレベルを循環する。
    前行のインデント → +4 → +8 → ... → 0 → 前行のインデント
    """
    global _last_indent_line, _last_indent_cycle

    win = alle.active_window()
    buf = win.buffer()
    point = win.point()
    line_index = buf.line_index_for_offset(point)

    # 前行のインデントを基準にする
    if line_index > 0:
        prev_indent_len = len(_get_indent(buf.line_text(line_index - 1)))
    else:
        prev_indent_len = 0

    current_indent_len = len(_get_indent(buf.line_text(line_index)))

    # 循環候補を構築（重複除去、0含む）
    candidates = []
    seen = set()
    for level in [prev_indent_len, prev_indent_len + _INDENT_UNIT,
                  max(prev_indent_len - _INDENT_UNIT, 0), 0]:
        if level not in seen:
            candidates.append(level)
            seen.add(level)

    # 連続TABの場合は循環
    if _last_indent_line == line_index and _last_indent_cycle < len(candidates):
        cycle_index = _last_indent_cycle
    else:
        # 現在のインデントが候補にあればその次から開始
        if current_indent_len in candidates:
            idx = candidates.index(current_indent_len)
            cycle_index = (idx + 1) % len(candidates)
        else:
            cycle_index = 0

    new_indent = " " * candidates[cycle_index]
    _set_line_indent(win, buf, line_index, new_indent)
    _last_indent_line = line_index
    _last_indent_cycle = (cycle_index + 1) % len(candidates)


@command("python-dedent-backspace")
def dedent_backspace():
    """行頭の空白領域でBackspace時にインデント単位で削除する。

    カーソルが行頭の空白領域にある場合、インデント1レベル分まとめて削除する。
    それ以外の位置では通常の1文字削除。
    """
    win = alle.active_window()
    buf = win.buffer()
    point = win.point()

    if point == 0:
        return

    line_index = buf.line_index_for_offset(point)
    line_start = buf.line_start_offset(line_index)
    col = point - line_start

    # カーソルが行頭の空白領域にあるか判定
    line_text = buf.line_text(line_index)
    indent = _get_indent(line_text)

    if col > 0 and col <= len(indent):
        # インデント単位で削除（最低1文字）
        delete_count = col % _INDENT_UNIT
        if delete_count == 0:
            delete_count = _INDENT_UNIT
        delete_count = min(delete_count, col)
        win.delete_backward(delete_count)
    else:
        win.delete_backward(1)


@command("python-comment-dwim")
def comment_dwim():
    """カーソル行のコメントをトグルする。

    行がコメント行（インデント後に ``# `` で始まる）なら ``# `` を削除する。
    そうでなければインデントの後に ``# `` を挿入する。
    """
    win = alle.active_window()
    buf = win.buffer()
    point = win.point()
    line_index = buf.line_index_for_offset(point)
    line_start = buf.line_start_offset(line_index)
    line_text = buf.line_text(line_index)
    indent = _get_indent(line_text)
    content_start = line_start + len(indent)

    if line_text[len(indent):].startswith("# "):
        # コメント解除: "# " を削除
        buf.delete_at(content_start, 2)
        win.goto_char(max(point - 2, line_start))
    elif line_text[len(indent):].startswith("#"):
        # "# " なしの "#" コメント解除
        buf.delete_at(content_start, 1)
        win.goto_char(max(point - 1, line_start))
    else:
        # コメント挿入: "# " を追加
        buf.insert_at(content_start, "# ")
        win.goto_char(point + 2)


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


@command("python-indent-region")
def indent_region():
    """選択範囲の各行をインデント1レベル増加する。"""
    win = alle.active_window()
    buf = win.buffer()
    start = win.region_start()
    end = win.region_end()
    if start < 0 or end < 0:
        alle.message("No region active")
        return

    start_line = buf.line_index_for_offset(start)
    end_line = buf.line_index_for_offset(end)
    indent_str = " " * _INDENT_UNIT

    # 末尾行から処理してオフセットのずれを防ぐ
    for li in range(end_line, start_line - 1, -1):
        ls = buf.line_start_offset(li)
        buf.insert_at(ls, indent_str)


@command("python-dedent-region")
def dedent_region():
    """選択範囲の各行をインデント1レベル減少する。"""
    win = alle.active_window()
    buf = win.buffer()
    start = win.region_start()
    end = win.region_end()
    if start < 0 or end < 0:
        alle.message("No region active")
        return

    start_line = buf.line_index_for_offset(start)
    end_line = buf.line_index_for_offset(end)

    for li in range(end_line, start_line - 1, -1):
        ls = buf.line_start_offset(li)
        line_text = buf.line_text(li)
        indent = _get_indent(line_text)
        remove = min(len(indent), _INDENT_UNIT)
        if remove > 0:
            buf.delete_at(ls, remove)


@command("python-comment-region")
def comment_region():
    """選択範囲の各行をコメントアウトする。全行がコメント済みなら解除する。"""
    win = alle.active_window()
    buf = win.buffer()
    start = win.region_start()
    end = win.region_end()
    if start < 0 or end < 0:
        alle.message("No region active")
        return

    start_line = buf.line_index_for_offset(start)
    end_line = buf.line_index_for_offset(end)

    # 全行がコメント済みかチェック
    all_commented = True
    for li in range(start_line, end_line + 1):
        line_text = buf.line_text(li)
        stripped = line_text.lstrip()
        if stripped and not stripped.startswith("#"):
            all_commented = False
            break

    if all_commented:
        # コメント解除（末尾行から）
        for li in range(end_line, start_line - 1, -1):
            line_text = buf.line_text(li)
            indent = _get_indent(line_text)
            ls = buf.line_start_offset(li)
            content_start = ls + len(indent)
            if line_text[len(indent):].startswith("# "):
                buf.delete_at(content_start, 2)
            elif line_text[len(indent):].startswith("#"):
                buf.delete_at(content_start, 1)
    else:
        # コメント挿入（末尾行から）
        for li in range(end_line, start_line - 1, -1):
            ls = buf.line_start_offset(li)
            line_text = buf.line_text(li)
            indent = _get_indent(line_text)
            content_start = ls + len(indent)
            buf.insert_at(content_start, "# ")
