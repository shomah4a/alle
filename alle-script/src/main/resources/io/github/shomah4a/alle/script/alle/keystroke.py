"""KeyStroke の Python ラッパー。"""

from __future__ import annotations

from typing import Any

import java

_Modifier: Any = java.type('io.github.shomah4a.alle.core.keybind.Modifier')


class KeyStroke:
    """Java 側の KeyStroke をラップし、Python 的なインターフェースを提供する。

    :param java_keystroke: Java 側の KeyStroke インスタンス
    :type java_keystroke: Any
    """

    def __init__(self, java_keystroke: Any) -> None:
        self._ks: Any = java_keystroke

    @property
    def key_code(self) -> int:
        """キーコードを返す。

        :return: キーコード（Unicode コードポイント）
        :rtype: int
        """
        return self._ks.keyCode()

    @property
    def modifiers(self) -> frozenset[str]:
        """修飾キーの集合を返す。

        :return: 修飾キー名の集合（``"ctrl"``, ``"meta"``, ``"shift"``）
        :rtype: frozenset[str]
        """
        result: set[str] = set()
        java_modifiers = self._ks.modifiers()
        if java_modifiers.contains(_Modifier.CTRL):
            result.add("ctrl")
        if java_modifiers.contains(_Modifier.META):
            result.add("meta")
        if java_modifiers.contains(_Modifier.SHIFT):
            result.add("shift")
        return frozenset(result)

    def has_ctrl(self) -> bool:
        """Ctrl 修飾キーが含まれるかを返す。

        :return: Ctrl が含まれる場合 True
        :rtype: bool
        """
        return self._ks.modifiers().contains(_Modifier.CTRL)

    def has_meta(self) -> bool:
        """Meta 修飾キーが含まれるかを返す。

        :return: Meta が含まれる場合 True
        :rtype: bool
        """
        return self._ks.modifiers().contains(_Modifier.META)

    def has_shift(self) -> bool:
        """Shift 修飾キーが含まれるかを返す。

        :return: Shift が含まれる場合 True
        :rtype: bool
        """
        return self._ks.modifiers().contains(_Modifier.SHIFT)

    def display_string(self) -> str:
        """Emacs 風の表示文字列を返す。

        例: ``"C-x"``, ``"M-x"``, ``"a"``

        :return: 表示文字列
        :rtype: str
        """
        return self._ks.displayString()

    def __repr__(self) -> str:
        return f"KeyStroke({self.display_string()!r})"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, KeyStroke):
            return NotImplemented
        return self._ks.equals(other._ks)

    def __hash__(self) -> int:
        return self._ks.hashCode()
