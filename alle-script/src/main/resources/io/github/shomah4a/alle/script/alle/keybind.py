"""キーバインド定義用ヘルパー。

KeyStroke の生成を Python 的に行うための関数を提供する。

使用例:
    from alle.keybind import ctrl, meta, key

    alle.global_set_key([ctrl('x'), ctrl('f')], find_file_cmd)
    alle.global_set_key([meta('x')], execute_cmd)
    alle.global_set_key([ctrl('s')], save_cmd)
"""

from __future__ import annotations

from typing import Any

import java

KeyStroke: Any = java.type('io.github.shomah4a.alle.core.keybind.KeyStroke')


def ctrl(char: str) -> Any:
    """Ctrl+キーの KeyStroke を返す。"""
    return KeyStroke.ctrl(ord(char))


def meta(char: str) -> Any:
    """Meta+キーの KeyStroke を返す。"""
    return KeyStroke.meta(ord(char))


def key(char: str) -> Any:
    """修飾キーなしの KeyStroke を返す。"""
    return KeyStroke.of(ord(char))
