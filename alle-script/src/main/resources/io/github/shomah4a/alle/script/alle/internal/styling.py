"""スタイリング関連の Java クラスラッパー。

Java 側の RegexStyler, StylingRule, FaceName 等を Python から
宣言的に利用するためのヘルパー関数を提供する。
"""

from __future__ import annotations

from typing import Any

import java

Pattern: Any = java.type('java.util.regex.Pattern')
FaceName: Any = java.type('io.github.shomah4a.alle.core.styling.FaceName')
FaceAttribute: Any = java.type('io.github.shomah4a.alle.core.styling.FaceAttribute')
_PatternMatch: Any = java.type('io.github.shomah4a.alle.core.styling.StylingRule$PatternMatch')
_LineMatch: Any = java.type('io.github.shomah4a.alle.core.styling.StylingRule$LineMatch')
_RegionMatch: Any = java.type('io.github.shomah4a.alle.core.styling.StylingRule$RegionMatch')
_RegexStyler: Any = java.type('io.github.shomah4a.alle.core.styling.RegexStyler')
_Lists: Any = java.type('org.eclipse.collections.api.factory.Lists')


def face_name(name: str, description: str) -> Any:
    """カスタムの FaceName を生成する。

    :param name: セマンティック名
    :type name: str
    :param description: 説明
    :type description: str
    :return: FaceName インスタンス
    :rtype: FaceName
    """
    return FaceName(name, description)


def pattern_match(regex: str, face_name_value: Any) -> Any:
    """部分パターンマッチルールを生成する。

    1行内でパターンにマッチした部分に FaceName を適用する。

    :param regex: 正規表現パターン文字列
    :type regex: str
    :param face_name_value: 適用する FaceName
    :type face_name_value: FaceName
    :return: StylingRule.PatternMatch インスタンス
    :rtype: StylingRule.PatternMatch
    """
    return _PatternMatch(Pattern.compile(regex), face_name_value)


def line_match(regex: str, face_name_value: Any) -> Any:
    """行全体マッチルールを生成する。

    パターンが行全体にマッチした場合、行全体に FaceName を適用する。

    :param regex: 正規表現パターン文字列
    :type regex: str
    :param face_name_value: 適用する FaceName
    :type face_name_value: FaceName
    :return: StylingRule.LineMatch インスタンス
    :rtype: StylingRule.LineMatch
    """
    return _LineMatch(Pattern.compile(regex), face_name_value)


def region_match(open_regex: str, close_regex: str, face_name_value: Any) -> Any:
    """リージョンマッチルールを生成する。

    開始パターンから終了パターンまでの複数行にまたがる領域に FaceName を適用する。

    :param open_regex: 開始パターンの正規表現文字列
    :type open_regex: str
    :param close_regex: 終了パターンの正規表現文字列
    :type close_regex: str
    :param face_name_value: 適用する FaceName
    :type face_name_value: FaceName
    :return: StylingRule.RegionMatch インスタンス
    :rtype: StylingRule.RegionMatch
    """
    return _RegionMatch(Pattern.compile(open_regex), Pattern.compile(close_regex), face_name_value)


def regex_styler(rules: list[Any]) -> Any:
    """StylingRule のリストから RegexStyler を生成する。

    :param rules: StylingRule のリスト
    :type rules: list[Any]
    :return: RegexStyler インスタンス
    :rtype: RegexStyler
    """
    mutable_list = _Lists.mutable.empty()
    for rule in rules:
        mutable_list.add(rule)
    return _RegexStyler(mutable_list)
