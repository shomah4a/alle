"""alle - エディタスクリプティングモジュール。

Java側のEditorFacadeをラップし、Python的なAPIを提供する。

使用例:
    import alle
    from alle.command import CommandBase
    from alle.keybind import ctrl, meta, key

    class MyCommand(CommandBase):
        def name(self):
            return "my-command"
        def run(self):
            alle.message("hello")

    cmd = MyCommand()
    alle.register_command(cmd)
    alle.global_set_key([ctrl('c'), key('h')], cmd)
"""

from __future__ import annotations

from typing import Any

from alle.buffer import Buffer
from alle.command import CommandBase
from alle.internal.facade import _init, _require_facade, _wrap_command
from alle.window import Window


def active_window() -> Window:
    """アクティブウィンドウを返す。"""
    return Window(_require_facade().activeWindow())


def current_buffer() -> Buffer:
    """カレントバッファを返す。"""
    return Buffer(_require_facade().currentBuffer())


def message(text: str) -> None:
    """エコーエリアにメッセージを表示する。"""
    _require_facade().message(text)


def register_command(command: CommandBase) -> None:
    """コマンドを登録する。同名のコマンドが既に存在する場合は上書きする。

    Args:
        command: CommandBase のサブクラスのインスタンス
    """
    _require_facade().registerCommand(_wrap_command(command))


def global_set_key(keystrokes: list[Any], command: CommandBase) -> None:
    """グローバルキーマップにキーバインドを設定する。

    Args:
        keystrokes: KeyStroke のリスト（例: [ctrl('x'), ctrl('f')]）
        command: CommandBase のサブクラスのインスタンス
    """
    _require_facade().globalSetKey(keystrokes, _wrap_command(command))
