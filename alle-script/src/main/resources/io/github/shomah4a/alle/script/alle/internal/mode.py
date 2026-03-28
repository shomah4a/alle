"""MajorModeBase / MinorModeBase から Java Mode への変換ブリッジ。"""

from __future__ import annotations

from typing import Any

import java
from java.util import Optional

from alle.mode import MajorModeBase, MinorModeBase

MajorMode: Any = java.type('io.github.shomah4a.alle.core.mode.MajorMode')
MinorMode: Any = java.type('io.github.shomah4a.alle.core.mode.MinorMode')


def _to_optional(value: Any | None) -> Any:
    """Python の None を Optional.empty() に、値を Optional.of() に変換する。

    :param value: 変換対象の値
    :type value: Any | None
    :return: Java の Optional
    :rtype: Optional
    """
    if value is None:
        return Optional.empty()
    return Optional.of(value)


def make_major_mode(base: MajorModeBase) -> Any:
    """MajorModeBase から Java MajorMode インスタンスを生成する。

    GraalPy では Java インターフェース継承クラスに __init__ 引数を渡せず、
    インスタンスへの属性追加もできないため、クロージャで値をキャプチャした
    クラスを都度生成する。

    :param base: 変換元のメジャーモード
    :type base: MajorModeBase
    :return: Java MajorMode インスタンス
    :rtype: MajorMode
    """
    mode_name = base.name()
    keymap_fn = base.keymap
    styler_fn = base.styler
    syntax_analyzer_fn = base.syntax_analyzer

    class _MajorMode(MajorMode):
        def name(self) -> str:
            return mode_name

        def keymap(self) -> Any:
            return _to_optional(keymap_fn())

        def styler(self) -> Any:
            return _to_optional(styler_fn())

        def syntaxAnalyzer(self) -> Any:
            return _to_optional(syntax_analyzer_fn())

    return _MajorMode()


def make_minor_mode(base: MinorModeBase) -> Any:
    """MinorModeBase から Java MinorMode インスタンスを生成する。

    GraalPy では Java インターフェース継承クラスに __init__ 引数を渡せず、
    インスタンスへの属性追加もできないため、クロージャで値をキャプチャした
    クラスを都度生成する。

    :param base: 変換元のマイナーモード
    :type base: MinorModeBase
    :return: Java MinorMode インスタンス
    :rtype: MinorMode
    """
    mode_name = base.name()
    keymap_fn = base.keymap

    class _MinorMode(MinorMode):
        def name(self) -> str:
            return mode_name

        def keymap(self) -> Any:
            return _to_optional(keymap_fn())

    return _MinorMode()
