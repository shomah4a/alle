"""Python メジャーモード。

.py / .pyw ファイルに適用される。
"""

from __future__ import annotations

from typing import Any

from alle.keybind import bind_key, create_keymap, key, shift
from alle.mode import MajorModeBase
from alle.modes.python.commands import PythonIndentState, create_indent_commands
from alle.modes.python.styler import create_python_analyzer, create_python_styler


class PythonMode(MajorModeBase):
    """Python のメジャーモード。"""

    def __init__(self) -> None:
        self._styler = create_python_styler()
        self._analyzer = create_python_analyzer()
        self._indent_state = PythonIndentState(self._analyzer)
        indent_line, dedent_line, newline_and_indent = create_indent_commands(self._indent_state)
        self._keymap = self._create_keymap(indent_line, dedent_line, newline_and_indent)

    @staticmethod
    def _create_keymap(indent_line, dedent_line, newline_and_indent) -> Any:
        """Python モード固有のキーマップを生成する。"""
        keymap = create_keymap("python-mode")
        bind_key(keymap, key("\n"), newline_and_indent)
        bind_key(keymap, key("\t"), indent_line)
        bind_key(keymap, shift("\t"), dedent_line)
        return keymap

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

        :return: SyntaxStyler インスタンス
        :rtype: SyntaxStyler
        """
        return self._styler

    def syntax_analyzer(self) -> Any:
        """Python の構文解析器を返す。

        :return: SyntaxAnalyzer インスタンス
        :rtype: SyntaxAnalyzer
        """
        return self._analyzer
