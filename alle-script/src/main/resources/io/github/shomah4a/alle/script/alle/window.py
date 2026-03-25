"""ウィンドウ操作のPythonラッパー。"""

from __future__ import annotations

from typing import Any


class Window:
    """WindowFacade のラッパー。"""

    def __init__(self, java_window_facade: Any) -> None:
        self._win: Any = java_window_facade

    def point(self) -> int:
        """カーソル位置を返す。"""
        return self._win.point()

    def goto_char(self, position: int) -> None:
        """カーソルを移動する。"""
        self._win.gotoChar(position)

    def insert(self, text: str) -> None:
        """テキストを挿入する。"""
        self._win.insert(text)

    def delete_backward(self, count: int) -> None:
        """前方にcount文字削除する。"""
        self._win.deleteBackward(count)

    def delete_forward(self, count: int) -> None:
        """後方にcount文字削除する。"""
        self._win.deleteForward(count)

    def buffer(self) -> Any:
        """このウィンドウのバッファを返す。"""
        from alle.buffer import Buffer
        return Buffer(self._win.buffer())
