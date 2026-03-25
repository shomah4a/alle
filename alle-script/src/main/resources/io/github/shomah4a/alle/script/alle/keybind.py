"""キーバインド定義用ヘルパー。

KeyStroke の生成とキーマップの操作を Python 的に行うための関数を提供する。

使用例:

>>> from alle.keybind import ctrl, meta, key, create_keymap, bind_key
>>> alle.global_set_key([ctrl('x'), ctrl('f')], find_file_cmd)
>>> alle.global_set_key([meta('x')], execute_cmd)
>>> alle.global_set_key([ctrl('s')], save_cmd)
>>> keymap = create_keymap("my-mode")
>>> bind_key(keymap, key('a'), my_command)
"""

from __future__ import annotations

from typing import Any

import java

from alle.command import CommandBase
from alle.internal.command import make_command

KeyStroke: Any = java.type('io.github.shomah4a.alle.core.keybind.KeyStroke')
_Keymap: Any = java.type('io.github.shomah4a.alle.core.keybind.Keymap')


def ctrl(char: str) -> Any:
    """Ctrl+キーの KeyStroke を返す。

    :param char: キー文字（例: ``'x'``）
    :type char: str
    :return: Ctrl 修飾付きの KeyStroke
    :rtype: KeyStroke
    """
    return KeyStroke.ctrl(ord(char))


def meta(char: str) -> Any:
    """Meta+キーの KeyStroke を返す。

    :param char: キー文字（例: ``'x'``）
    :type char: str
    :return: Meta 修飾付きの KeyStroke
    :rtype: KeyStroke
    """
    return KeyStroke.meta(ord(char))


def key(char: str) -> Any:
    """修飾キーなしの KeyStroke を返す。

    :param char: キー文字（例: ``'a'``）
    :type char: str
    :return: 修飾なしの KeyStroke
    :rtype: KeyStroke
    """
    return KeyStroke.of(ord(char))


def create_keymap(name: str) -> Any:
    """新しいキーマップを生成する。

    :param name: キーマップの識別名
    :type name: str
    :return: Keymap インスタンス
    :rtype: Keymap
    """
    return _Keymap(name)


def bind_key(keymap: Any, keystroke: Any, command: CommandBase) -> None:
    """キーマップにコマンドをバインドする。

    CommandBase を Java Command に変換してからバインドする。

    :param keymap: バインド先のキーマップ
    :type keymap: Keymap
    :param keystroke: バインドするキーストローク
    :type keystroke: KeyStroke
    :param command: バインドするコマンド
    :type command: CommandBase
    """
    java_command = make_command(command)
    keymap.bind(keystroke, java_command)


def bind_prefix(keymap: Any, keystroke: Any, prefix_keymap: Any) -> None:
    """キーマップにプレフィックスキーをバインドする。

    :param keymap: バインド先のキーマップ
    :type keymap: Keymap
    :param keystroke: プレフィックスとなるキーストローク
    :type keystroke: KeyStroke
    :param prefix_keymap: プレフィックスキー用の子キーマップ
    :type prefix_keymap: Keymap
    """
    keymap.bindPrefix(keystroke, prefix_keymap)
