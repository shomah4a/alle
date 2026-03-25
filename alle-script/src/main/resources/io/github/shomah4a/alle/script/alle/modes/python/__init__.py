"""Python メジャーモード。

.py / .pyw ファイルに適用される。
"""

from __future__ import annotations

from typing import Any

from alle.keybind import bind_key, bind_prefix, create_keymap, ctrl, key, meta
from alle.mode import MajorModeBase
from alle.modes.python.commands import (
    comment_dwim,
    comment_region,
    dedent_backspace,
    dedent_region,
    indent_line,
    indent_region,
    newline_and_indent,
)
from alle.modes.python.styler import create_python_styler


def _create_python_keymap() -> Any:
    """Python モード固有のキーマップを生成する。

    :return: Keymap インスタンス
    :rtype: Keymap
    """
    keymap = create_keymap("python-mode")
    bind_key(keymap, key("\n"), newline_and_indent)
    bind_key(keymap, key("\t"), indent_line)
    bind_key(keymap, key("\x7f"), dedent_backspace)
    bind_key(keymap, meta(";"), comment_dwim)

    # C-c プレフィックスキー
    cc_map = create_keymap("C-c")
    bind_key(cc_map, key(">"), indent_region)
    bind_key(cc_map, key("<"), dedent_region)
    bind_key(cc_map, key("#"), comment_region)
    bind_prefix(keymap, ctrl("c"), cc_map)

    return keymap


class PythonMode(MajorModeBase):
    """Python のメジャーモード。"""

    def __init__(self) -> None:
        self._styler = create_python_styler()
        self._keymap = _create_python_keymap()

    def name(self) -> str:
        """モード名を返す。

        :return: "Python"
        :rtype: str
        """
        return "Python"

    def keymap(self) -> Any:
        """Python モード固有のキーマップを返す。

        :return: Keymap インスタンス
        :rtype: Keymap
        """
        return self._keymap

    def styler(self) -> Any:
        """Python のシンタックススタイラーを返す。

        :return: RegexStyler インスタンス
        :rtype: RegexStyler
        """
        return self._styler
