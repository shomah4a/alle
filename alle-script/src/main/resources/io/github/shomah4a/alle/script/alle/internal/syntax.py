"""構文解析関連の Java クラスラッパー。

Java 側の SyntaxAnalyzer, SyntaxTree 等を Python から利用するためのヘルパー関数を提供する。
"""

from __future__ import annotations

from typing import Any


def syntax_analyzer(language: str) -> Any:
    """指定言語の構文解析器を取得する。

    :param language: 言語名（例: "python"）
    :type language: str
    :return: SyntaxAnalyzer インスタンス
    :rtype: SyntaxAnalyzer
    :raises ValueError: 未対応の言語が指定された場合
    """
    from alle.internal.facade import _require_facade
    analyzer = _require_facade().createSyntaxAnalyzer(language)
    if analyzer is None:
        raise ValueError(f"未対応の言語です: {language}")
    return analyzer
