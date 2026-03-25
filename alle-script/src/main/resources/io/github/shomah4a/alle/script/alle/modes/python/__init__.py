"""Python メジャーモード。

.py / .pyw ファイルに適用される。
"""

from __future__ import annotations

from typing import Any

from alle.keybind import bind_key, create_keymap, key
from alle.mode import MajorModeBase
from alle.modes.python.commands import indent_line, newline_and_indent
from alle.modes.python.styler import create_python_styler


def _create_python_keymap() -> Any:
    """Python モード固有のキーマップを生成する。

    Python 固有のコマンドのみバインドする。
    コアコマンド（comment-dwim, indent-region 等）はグローバルキーマップにバインドされている。

    :return: Keymap インスタンス
    :rtype: Keymap
    """
    keymap = create_keymap("python-mode")
    bind_key(keymap, key("\n"), newline_and_indent)
    bind_key(keymap, key("\t"), indent_line)
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
