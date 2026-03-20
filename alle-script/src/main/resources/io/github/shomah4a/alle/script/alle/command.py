"""コマンド定義。

ユーザーは CommandBase を継承してコマンドを定義する。
"""

from __future__ import annotations

from abc import ABC, abstractmethod


class CommandBase(ABC):
    """コマンドの基底クラス。

    サブクラスは name() と run() を実装する。

    使用例:
        class MyCommand(CommandBase):
            def name(self):
                return "my-command"
            def run(self):
                alle.message("hello")

        alle.register_command(MyCommand())
    """

    @abstractmethod
    def name(self) -> str:
        """コマンド名を返す。"""
        ...

    @abstractmethod
    def run(self) -> None:
        """コマンドの実行内容。"""
        ...
