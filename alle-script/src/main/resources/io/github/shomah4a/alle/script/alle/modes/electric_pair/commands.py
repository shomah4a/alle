"""Electric Pair モードのコマンド定義。"""

from __future__ import annotations

import alle
from alle.command import command

_PAIR_MAP = {")": "(", "]": "[", "}": "{"}


def _insert_pair(open_char: str, close_char: str) -> None:
    """開き文字と閉じ文字のペアを挿入し、カーソルをペアの間に置く。

    :param open_char: 開き文字
    :type open_char: str
    :param close_char: 閉じ文字
    :type close_char: str
    """
    win = alle.active_window()
    win.insert(open_char + close_char)
    win.goto_char(win.point() - 1)


def _has_unmatched_open(text: str, open_char: str, close_char: str) -> bool:
    """テキスト中に未対応の開き括弧があるかを判定する。

    簡易的なカウント方式で、文字列リテラル内の括弧は考慮しない。

    :param text: 判定対象のテキスト
    :type text: str
    :param open_char: 開き括弧文字
    :type open_char: str
    :param close_char: 閉じ括弧文字
    :type close_char: str
    :return: 未対応の開き括弧があれば True
    :rtype: bool
    """
    depth = 0
    for ch in text:
        if ch == open_char:
            depth += 1
        elif ch == close_char:
            depth -= 1
    return depth > 0


def _skip_or_insert_close(close_char: str) -> None:
    """閉じ文字を入力する。

    次の文字が同じ閉じ文字かつ、カーソルより前に未対応の開き括弧がある場合は
    スキップしてカーソルを移動する。そうでなければ閉じ文字を挿入する。

    :param close_char: 閉じ文字
    :type close_char: str
    """
    win = alle.active_window()
    buf = win.buffer()
    point = win.point()

    if point < buf.length():
        next_char = buf.substring(point, point + 1)
        if next_char == close_char:
            open_char = _PAIR_MAP[close_char]
            text_before = buf.substring(0, point)
            if _has_unmatched_open(text_before, open_char, close_char):
                win.goto_char(point + 1)
                return

    win.insert(close_char)


def _is_word_char(ch: str) -> bool:
    """文字が単語構成文字（英数字またはアンダースコア）かを判定する。

    :param ch: 判定対象の文字
    :type ch: str
    :return: 単語構成文字なら True
    :rtype: bool
    """
    return ch.isalnum() or ch == "_"


def _insert_quote(quote_char: str) -> None:
    """クォート文字を入力する。

    次の文字が同じクォートならスキップする。
    カーソル左側が単語構成文字（英数字やアンダースコア）の場合は
    ペア挿入せず単一文字を挿入する（``it's`` のようなケースへの対応）。
    それ以外の場合はペアを挿入してカーソルをペアの間に置く。

    :param quote_char: クォート文字
    :type quote_char: str
    """
    win = alle.active_window()
    buf = win.buffer()
    point = win.point()

    # 次の文字が同じクォートならスキップ
    if point < buf.length():
        next_char = buf.substring(point, point + 1)
        if next_char == quote_char:
            win.goto_char(point + 1)
            return

    # カーソル左側が単語構成文字ならペア挿入しない
    if point > 0:
        prev_char = buf.substring(point - 1, point)
        if _is_word_char(prev_char):
            win.insert(quote_char)
            return

    win.insert(quote_char + quote_char)
    win.goto_char(win.point() - 1)


@command("electric-open-paren")
def open_paren():
    """``(`` を入力し、 ``)`` を自動挿入する。"""
    _insert_pair("(", ")")


@command("electric-close-paren")
def close_paren():
    """次の文字が ``)`` ならスキップし、そうでなければ ``)`` を挿入する。"""
    _skip_or_insert_close(")")


@command("electric-open-bracket")
def open_bracket():
    """``[`` を入力し、 ``]`` を自動挿入する。"""
    _insert_pair("[", "]")


@command("electric-close-bracket")
def close_bracket():
    """次の文字が ``]`` ならスキップし、そうでなければ ``]`` を挿入する。"""
    _skip_or_insert_close("]")


@command("electric-open-brace")
def open_brace():
    """``{`` を入力し、 ``}`` を自動挿入する。"""
    _insert_pair("{", "}")


@command("electric-close-brace")
def close_brace():
    """次の文字が ``}`` ならスキップし、そうでなければ ``}`` を挿入する。"""
    _skip_or_insert_close("}")


@command("electric-double-quote")
def insert_double_quote():
    """``"`` のペアを挿入するか、次の ``"`` をスキップする。"""
    _insert_quote('"')


@command("electric-single-quote")
def insert_single_quote():
    """``'`` のペアを挿入するか、次の ``'`` をスキップする。"""
    _insert_quote("'")
