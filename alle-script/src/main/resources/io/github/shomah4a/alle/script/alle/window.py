"""ウィンドウ操作のPythonラッパー。"""

from __future__ import annotations

from typing import Any

from alle.futures import JavaFuture
from alle.internal.futures import wrap, wrap_transform


class Window:
    """WindowFacade のラッパー。"""

    def __init__(self, java_window_facade: Any) -> None:
        self._win: Any = java_window_facade

    def point(self) -> JavaFuture:
        """カーソル位置を返す (JavaFuture[int])。"""
        return wrap(self._win.point())

    def goto_char(self, position: int) -> JavaFuture:
        """カーソルを移動する (JavaFuture[None])。"""
        return wrap(self._win.gotoChar(position))

    def insert(self, text: str) -> JavaFuture:
        """テキストを挿入する (JavaFuture[None])。"""
        return wrap(self._win.insert(text))

    def delete_backward(self, count: int) -> JavaFuture:
        """前方にcount文字削除する (JavaFuture[None])。"""
        return wrap(self._win.deleteBackward(count))

    def delete_forward(self, count: int) -> JavaFuture:
        """後方にcount文字削除する (JavaFuture[None])。"""
        return wrap(self._win.deleteForward(count))

    def buffer(self) -> JavaFuture:
        """このウィンドウのバッファを返す (JavaFuture[Buffer])。"""
        from alle.buffer import Buffer
        return wrap_transform(self._win.buffer(), lambda b: Buffer(b))
