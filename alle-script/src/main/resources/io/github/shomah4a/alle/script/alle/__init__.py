"""alle - エディタスクリプティングモジュール。

Java側のEditorFacadeをラップし、Python的なAPIを提供する。

使用例:

>>> import alle
>>> from alle.command import CommandBase
>>> from alle.keybind import ctrl, meta, key
>>> class MyCommand(CommandBase):
...     def name(self):
...         return "my-command"
...     def run(self):
...         alle.message("hello")
>>> cmd = MyCommand()
>>> alle.register_command(cmd)
>>> alle.global_set_key([ctrl('c'), key('h')], cmd)
"""

from __future__ import annotations

from typing import Any

from alle.buffer import Buffer
from alle.command import CommandBase
from alle.internal.facade import (
    _init,
    _make_major_mode_factory,
    _make_minor_mode_factory,
    _require_facade,
    _wrap_command,
)
from alle.mode import MajorModeBase, MinorModeBase
from alle.window import Window


def active_window() -> Window:
    """アクティブウィンドウを返す。

    :return: アクティブな Window
    :rtype: Window
    """
    return Window(_require_facade().activeWindow())


def current_buffer() -> Buffer:
    """カレントバッファを返す。

    :return: カレントバッファ
    :rtype: Buffer
    """
    return Buffer(_require_facade().currentBuffer())


def message(text: str) -> None:
    """エコーエリアにメッセージを表示する。

    :param text: 表示するメッセージ文字列
    :type text: str
    """
    _require_facade().message(text)


def register_command(command: CommandBase) -> None:
    """コマンドを登録する。同名のコマンドが既に存在する場合は上書きする。

    :param command: CommandBase のサブクラスのインスタンス
    :type command: CommandBase
    """
    _require_facade().registerCommand(_wrap_command(command))


def global_set_key(keystrokes: list[Any], command: CommandBase) -> None:
    """グローバルキーマップにキーバインドを設定する。

    :param keystrokes: KeyStroke のリスト（例: ``[ctrl('x'), ctrl('f')]``）
    :type keystrokes: list[Any]
    :param command: CommandBase のサブクラスのインスタンス
    :type command: CommandBase
    """
    _require_facade().globalSetKey(keystrokes, _wrap_command(command))


def register_major_mode(
    mode_class: type[MajorModeBase],
    extensions: list[str] | None = None,
) -> None:
    """メジャーモードを登録する。同名のモードが既に存在する場合は上書きする。

    :param mode_class: MajorModeBase のサブクラス（クラスそのもの）
    :type mode_class: type[MajorModeBase]
    :param extensions: 関連付けるファイル拡張子のリスト（ドットなし）。
        指定した場合、拡張子→モード名のマッピングも自動登録する。
    :type extensions: list[str] | None

    使用例:

    >>> from alle.mode import MajorModeBase
    >>> class PythonMode(MajorModeBase):
    ...     def name(self):
    ...         return "Python"
    >>> alle.register_major_mode(PythonMode, extensions=["py", "pyw"])
    """
    facade = _require_facade()
    factory = _make_major_mode_factory(mode_class)
    facade.registerMajorMode(factory)
    if extensions:
        mode_name = mode_class().name()
        for ext in extensions:
            facade.registerAutoMode(ext, mode_name)


def register_minor_mode(mode_class: type[MinorModeBase]) -> None:
    """マイナーモードを登録する。同名のモードが既に存在する場合は上書きする。

    :param mode_class: MinorModeBase のサブクラス（クラスそのもの）
    :type mode_class: type[MinorModeBase]

    使用例:

    >>> from alle.mode import MinorModeBase
    >>> class AutoSaveMode(MinorModeBase):
    ...     def name(self):
    ...         return "AutoSave"
    >>> alle.register_minor_mode(AutoSaveMode)
    """
    facade = _require_facade()
    factory = _make_minor_mode_factory(mode_class)
    facade.registerMinorMode(factory)
