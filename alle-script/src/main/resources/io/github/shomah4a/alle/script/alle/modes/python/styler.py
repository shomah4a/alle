"""Python のシンタックススタイリングおよび構文解析定義。"""

from __future__ import annotations

from typing import Any

from alle.internal.styling import parser_styler
from alle.internal.syntax import syntax_analyzer


def create_python_styler() -> Any:
    """Python 用のパーサーベーススタイラーを生成する。

    :return: SyntaxStyler インスタンス
    :rtype: SyntaxStyler
    """
    return parser_styler("python")


def create_python_analyzer() -> Any:
    """Python 用の構文解析器を生成する。

    :return: SyntaxAnalyzer インスタンス
    :rtype: SyntaxAnalyzer
    """
    return syntax_analyzer("python")
