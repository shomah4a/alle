"""モード基底クラス。

ユーザーは MajorModeBase / MinorModeBase を継承してモードを定義する。

使用例:

>>> from alle.mode import MajorModeBase
>>> class MyMode(MajorModeBase):
...     def name(self):
...         return "my-mode"
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any


class MajorModeBase(ABC):
    """メジャーモードの基底クラス。

    サブクラスは name() を実装する。
    keymap() / styler() / commands() はデフォルトで None / 空リストを返す。
    必要に応じてオーバーライドする。
    """

    @abstractmethod
    def name(self) -> str:
        """モード名を返す。

        :return: モードの識別名
        :rtype: str
        """
        ...

    def keymap(self) -> Any | None:
        """モード固有のキーマップを返す。不要なら None。

        :return: キーマップ、または None
        :rtype: Any | None
        """
        return None

    def styler(self) -> Any | None:
        """シンタックススタイラーを返す。不要なら None。

        :return: スタイラー、または None
        :rtype: Any | None
        """
        return None

    def syntax_analyzer(self) -> Any | None:
        """構文解析器を返す。不要なら None。

        :return: SyntaxAnalyzer、または None
        :rtype: Any | None
        """
        return None

    def commands(self) -> list:
        """モード固有のコマンド（CommandBase）のリストを返す。

        :return: CommandBase インスタンスのリスト
        :rtype: list
        """
        return []


class MinorModeBase(ABC):
    """マイナーモードの基底クラス。

    サブクラスは name() を実装する。
    keymap() / commands() はデフォルトで None / 空リストを返す。
    必要に応じてオーバーライドする。
    """

    @abstractmethod
    def name(self) -> str:
        """モード名を返す。

        :return: モードの識別名
        :rtype: str
        """
        ...

    def keymap(self) -> Any | None:
        """モード固有のキーマップを返す。不要なら None。

        :return: キーマップ、または None
        :rtype: Any | None
        """
        return None

    def commands(self) -> list:
        """モード固有のコマンド（CommandBase）のリストを返す。

        :return: CommandBase インスタンスのリスト
        :rtype: list
        """
        return []
