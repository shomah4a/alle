"""Python モード固有のコマンド定義。"""

from __future__ import annotations

import re

import alle
from alle.command import command

# コロンで終わる行のパターン（コメント・文字列内のコロンは除外）
# 行末の空白とコメントを除去した上で : で終わるかを判定する
_COLON_END = re.compile(r":\s*(?:#.*)?$")

# 行頭の空白を取得するパターン
_LEADING_WHITESPACE = re.compile(r"^(\s*)")


@command("python-newline-and-indent")
def newline_and_indent():
    """改行を挿入し、前行のインデントを継承する。

    前行が ``:`` で終わる場合はインデントを1レベル（4スペース）増加する。
    """
    win = alle.active_window()
    buf = win.buffer()
    point = win.point()

    line_index = buf.line_index_for_offset(point)
    line_start = buf.line_start_offset(line_index)
    # カーソル位置より左側のテキストで判定する
    text_before_cursor = buf.substring(line_start, point)

    # 前行のインデントを取得
    match = _LEADING_WHITESPACE.match(text_before_cursor)
    indent = match.group(1) if match else ""

    # コロンで終わる場合はインデントを増やす
    if _COLON_END.search(text_before_cursor):
        indent += "    "

    # 改行 + インデントを1回の insert で挿入（undo 粒度を保つ）
    win.insert("\n" + indent)
