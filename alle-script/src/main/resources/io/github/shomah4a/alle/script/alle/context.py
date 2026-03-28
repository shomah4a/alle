"""コマンド実行コンテキストのPythonラッパー。"""

from __future__ import annotations

from typing import Any

from alle.buffer import Buffer
from alle.window import Window


class CommandContext:
    """ScriptCommandContext のラッパー。

    コマンドの run(ctx) に渡される。

    :param java_ctx: Java 側の ScriptCommandContext インスタンス
    :type java_ctx: Any
    """

    def __init__(self, java_ctx: Any) -> None:
        self._ctx: Any = java_ctx

    def active_window(self) -> Window:
        """アクティブウィンドウを返す。

        :return: アクティブな Window
        :rtype: Window
        """
        return Window(self._ctx.activeWindow())

    def current_buffer(self) -> Buffer:
        """カレントバッファを返す。

        :return: カレントバッファ
        :rtype: Buffer
        """
        return Buffer(self._ctx.currentBuffer())

    def message(self, text: str) -> None:
        """エコーエリアにメッセージを表示する。

        :param text: 表示するメッセージ文字列
        :type text: str
        """
        self._ctx.message(text)
