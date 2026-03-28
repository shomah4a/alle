"""Python のシンタックススタイリングルール定義。"""

from __future__ import annotations

from typing import Any

from alle.internal.styling import (
    FaceName,
    line_match,
    pattern_match,
    regex_styler,
    region_match,
)

# Python キーワード
_KEYWORDS = (
    "False|None|True|and|as|assert|async|await|break|class|continue|"
    "def|del|elif|else|except|finally|for|from|global|if|import|in|"
    "is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield"
)

# 組み込み関数
_BUILTINS = (
    "print|len|range|type|int|str|float|list|dict|set|tuple|bool|"
    "isinstance|issubclass|hasattr|getattr|setattr|delattr|"
    "enumerate|zip|map|filter|sorted|reversed|"
    "open|input|super|property|staticmethod|classmethod|"
    "abs|all|any|bin|chr|dir|divmod|format|hash|hex|id|iter|"
    "max|min|next|oct|ord|pow|repr|round|sum|vars"
)


def create_python_styler() -> Any:
    """Python 用の RegexStyler を生成する。

    :return: RegexStyler インスタンス
    :rtype: RegexStyler
    """
    rules = [
        # 三重引用符文字列（複数行リージョン）
        region_match('"""', '"""', FaceName.STRING),
        region_match("'''", "'''", FaceName.STRING),
        # コメント（# から行末まで）
        pattern_match("#.*$", FaceName.COMMENT),
        # 文字列（単一行）
        pattern_match('"[^"\\\\]*(?:\\\\.[^"\\\\]*)*"', FaceName.STRING),
        pattern_match("'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'", FaceName.STRING),
        # デコレータ（@から行末の識別子まで）
        pattern_match("@[a-zA-Z_][a-zA-Z0-9_.]*", FaceName.ANNOTATION),
        # キーワード（単語境界付き）
        pattern_match(f"\\b(?:{_KEYWORDS})\\b", FaceName.KEYWORD),
        # 組み込み関数（単語境界付き、呼び出しコンテキスト）
        pattern_match(f"\\b(?:{_BUILTINS})\\b(?=\\s*\\()", FaceName.KEYWORD),
        # 数値（整数、浮動小数点、16進、8進、2進）
        pattern_match(
            "\\b(?:0[xX][0-9a-fA-F_]+|0[oO][0-7_]+|0[bB][01_]+|"
            "[0-9][0-9_]*(?:\\.[0-9_]*)?(?:[eE][+-]?[0-9_]+)?j?)\\b",
            FaceName.NUMBER,
        ),
        # self / cls パラメータ
        pattern_match("\\bself\\b|\\bcls\\b", FaceName.KEYWORD),
    ]
    return regex_styler(rules)
