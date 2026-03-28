"""Python のシンタックススタイリングおよび構文解析定義。"""

from __future__ import annotations

from typing import Any


def _get_language_support() -> Any:
    """Python 用の言語サポート（スタイラーとアナライザーの組）を取得する。

    :return: LanguageSupport インスタンス
    :rtype: LanguageSupport
    :raises ValueError: 未対応の言語が指定された場合
    """
    from alle.internal.facade import _require_facade
    support = _require_facade().createLanguageSupport("python")
    if support is None:
        raise ValueError("未対応の言語です: python")
    return support


def create_python_styler_and_analyzer() -> tuple[Any, Any]:
    """Python 用のスタイラーとアナライザーを生成する。

    同一セッションを共有するため、必ずペアで生成する。

    :return: (SyntaxStyler, SyntaxAnalyzer) のタプル
    :rtype: tuple[Any, Any]
    """
    support = _get_language_support()
    return support.styler(), support.analyzer()
