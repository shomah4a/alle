"""コマンド実行コンテキストのPythonラッパー。"""

from __future__ import annotations

from typing import Any

from alle.buffer import Buffer
from alle.keystroke import KeyStroke
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

    def triggering_key_sequence(self) -> list[KeyStroke]:
        """コマンドを発動したキーシーケンスを返す。

        プログラム的呼び出し時は空リストを返す。
        単一キー（例: ``a``）は要素1つのリスト、
        プレフィックスキー（例: ``C-x C-f``）は要素2つのリストとなる。

        :return: キーシーケンス
        :rtype: list[KeyStroke]
        """
        java_sequence = self._ctx.triggeringKeySequence()
        return [KeyStroke(java_sequence.get(i)) for i in range(java_sequence.size())]

    def delegate(self, command_name: str) -> None:
        """名前を指定して別のコマンドを実行する。

        コンテキストはそのまま渡されるため、this_commandやlast_commandは変わらない。

        :param command_name: 実行するコマンド名
        :type command_name: str
        :raises IllegalArgumentException: 指定された名前のコマンドが登録されていない場合
        """
        self._ctx.delegate(command_name).join()
