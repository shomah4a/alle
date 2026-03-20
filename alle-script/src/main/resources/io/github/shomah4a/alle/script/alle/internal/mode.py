"""MajorModeBase / MinorModeBase から Java Mode への変換ブリッジ。"""

from __future__ import annotations

from typing import Any

import java
from java.util import Optional

from alle.mode import MajorModeBase, MinorModeBase

MajorMode: Any = java.type('io.github.shomah4a.alle.core.mode.MajorMode')
MinorMode: Any = java.type('io.github.shomah4a.alle.core.mode.MinorMode')


def _to_optional(value: Any | None) -> Any:
    """Python の None を Optional.empty() に、値を Optional.of() に変換する。"""
    if value is None:
        return Optional.empty()
    return Optional.of(value)


def make_major_mode(base: MajorModeBase) -> Any:
    """MajorModeBase から Java MajorMode インスタンスを生成する。

    GraalPy では Java インターフェース継承クラスに __init__ 引数を渡せず、
    インスタンスへの属性追加もできないため、クロージャで値をキャプチャした
    クラスを都度生成する。
    """
    mode_name = base.name()
    keymap_fn = base.keymap
    highlighter_fn = base.highlighter

    class _MajorMode(MajorMode):
        def name(self) -> str:
            return mode_name

        def keymap(self) -> Any:
            return _to_optional(keymap_fn())

        def highlighter(self) -> Any:
            return _to_optional(highlighter_fn())

    return _MajorMode()


def make_minor_mode(base: MinorModeBase) -> Any:
    """MinorModeBase から Java MinorMode インスタンスを生成する。

    GraalPy では Java インターフェース継承クラスに __init__ 引数を渡せず、
    インスタンスへの属性追加もできないため、クロージャで値をキャプチャした
    クラスを都度生成する。
    """
    mode_name = base.name()
    keymap_fn = base.keymap

    class _MinorMode(MinorMode):
        def name(self) -> str:
            return mode_name

        def keymap(self) -> Any:
            return _to_optional(keymap_fn())

    return _MinorMode()
