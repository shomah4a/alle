"""ウィンドウ操作のPythonラッパー。"""

from __future__ import annotations

from typing import Any


class Window:
    """WindowFacade のラッパー。

    :param java_window_facade: Java 側の WindowFacade インスタンス
    :type java_window_facade: Any
    """

    def __init__(self, java_window_facade: Any) -> None:
        self._win: Any = java_window_facade

    def point(self) -> int:
        """カーソル位置を返す。

        :return: カーソルのオフセット位置
        :rtype: int
        """
        return self._win.point()

    def goto_char(self, position: int) -> None:
        """カーソルを移動する。

        :param position: 移動先のオフセット位置
        :type position: int
        """
        self._win.gotoChar(position)

    def insert(self, text: str) -> None:
        """カーソル位置にテキストを挿入する。

        :param text: 挿入するテキスト
        :type text: str
        """
        self._win.insert(text)

    def delete_backward(self, count: int) -> None:
        """カーソルの前方をcount文字削除する。

        :param count: 削除する文字数
        :type count: int
        """
        self._win.deleteBackward(count)

    def delete_forward(self, count: int) -> None:
        """カーソルの後方をcount文字削除する。

        :param count: 削除する文字数
        :type count: int
        """
        self._win.deleteForward(count)

    def buffer(self) -> Any:
        """このウィンドウのバッファを返す。

        :return: ウィンドウに関連付けられた Buffer
        :rtype: Buffer
        """
        from alle.buffer import Buffer
        return Buffer(self._win.buffer())
