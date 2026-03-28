"""Python のシンタックススタイリング定義。"""

from __future__ import annotations

from typing import Any

from alle.internal.styling import parser_styler


def create_python_styler() -> Any:
    """Python 用のパーサーベーススタイラーを生成する。

    :return: SyntaxStyler インスタンス
    :rtype: SyntaxStyler
    """
    return parser_styler("python")
