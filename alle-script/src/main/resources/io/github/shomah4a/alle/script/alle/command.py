"""コマンド定義。

ユーザーは CommandBase を継承するか、command デコレータを使ってコマンドを定義する。
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Callable


class CommandBase(ABC):
    """コマンドの基底クラス。

    サブクラスは name() と run() を実装する。

    使用例:

    >>> class MyCommand(CommandBase):
    ...     def name(self):
    ...         return "my-command"
    ...     def run(self):
    ...         alle.message("hello")
    >>> alle.register_command(MyCommand())
    """

    @abstractmethod
    def name(self) -> str:
        """コマンド名を返す。

        :return: コマンドの識別名
        :rtype: str
        """
        ...

    @abstractmethod
    def run(self) -> None:
        """コマンドの実行内容。"""
        ...


def command(name: str) -> Callable[[Callable[[], None]], CommandBase]:
    """関数をコマンドに変換するデコレータ。

    デコレータ適用後の変数は関数ではなく CommandBase インスタンスになる。

    :param name: コマンドの識別名
    :type name: str
    :return: 関数を CommandBase インスタンスに変換するデコレータ
    :rtype: Callable[[Callable[[], None]], CommandBase]

    使用例:

    >>> from alle.command import command
    >>> @command("my-command")
    ... def my_command():
    ...     alle.message("hello")
    >>> # my_command は CommandBase インスタンス
    >>> alle.register_command(my_command)
    """
    def decorator(fn: Callable[[], None]) -> CommandBase:
        cmd_name = name
        class _Cmd(CommandBase):
            def name(self) -> str:
                return cmd_name
            def run(self) -> None:
                fn()
        return _Cmd()
    return decorator
