"""コマンド定義。

ユーザーは CommandBase を継承してコマンドを定義する。
make_command() が CommandBase を Java の Command インターフェースに適合させる。
"""

from abc import ABC, abstractmethod

import java
from java.util.concurrent import CompletableFuture

Command = java.type('io.github.shomah4a.alle.core.command.Command')


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
    def name(self):
        """コマンド名を返す。"""
        ...

    @abstractmethod
    def run(self):
        """コマンドの実行内容。"""
        ...


def make_command(base):
    """CommandBase から Java Command インスタンスを生成する。

    GraalPy では Java インターフェース継承クラスに __init__ 引数を渡せず、
    インスタンスへの属性追加もできないため、クロージャで値をキャプチャした
    クラスを都度生成する。
    """
    cmd_name = base.name()
    run_fn = base.run

    class _Cmd(Command):
        def name(self):
            return cmd_name

        def execute(self, ctx):
            try:
                run_fn()
                return CompletableFuture.completedFuture(None)
            except Exception as e:
                return CompletableFuture.failedFuture(e)

    return _Cmd()
