"""CommandBase から Java Command への変換ブリッジ。"""

from __future__ import annotations

from typing import Any

import java

from alle.command import CommandBase
from alle.context import CommandContext

ScriptCommand: Any = java.type('io.github.shomah4a.alle.script.ScriptCommand')


def make_command(base: CommandBase) -> Any:
    """CommandBase から Java ScriptCommand インスタンスを生成する。

    Java側のScriptCommandがCommandContextをScriptCommandContextに変換し、
    Python側でさらにCommandContextラッパーに包んでからrun関数を呼び出す。

    :param base: 変換元のコマンド
    :type base: CommandBase
    :return: Java Command インスタンス
    :rtype: Command
    """
    run_fn = base.run

    def wrapped_run(java_script_ctx: Any) -> None:
        ctx = CommandContext(java_script_ctx)
        run_fn(ctx)

    return ScriptCommand(base.name(), wrapped_run)
