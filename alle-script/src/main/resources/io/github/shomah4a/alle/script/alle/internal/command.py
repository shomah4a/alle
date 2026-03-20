"""CommandBase から Java Command への変換ブリッジ。"""

from __future__ import annotations

from typing import Any

import java
from java.util.concurrent import CompletableFuture

from alle.command import CommandBase

Command: Any = java.type('io.github.shomah4a.alle.core.command.Command')


def make_command(base: CommandBase) -> Any:
    """CommandBase から Java Command インスタンスを生成する。

    GraalPy では Java インターフェース継承クラスに __init__ 引数を渡せず、
    インスタンスへの属性追加もできないため、クロージャで値をキャプチャした
    クラスを都度生成する。
    """
    cmd_name = base.name()
    run_fn = base.run

    class _Cmd(Command):
        def name(self) -> str:
            return cmd_name

        def execute(self, ctx: Any) -> Any:
            try:
                run_fn()
                return CompletableFuture.completedFuture(None)
            except Exception as e:
                return CompletableFuture.failedFuture(e)

    return _Cmd()
