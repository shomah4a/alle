"""ウィンドウ操作のPythonラッパー。"""

from alle.futures import wrap, wrap_transform


class Window:
    """WindowFacade のラッパー。"""

    def __init__(self, java_window_facade):
        self._win = java_window_facade

    def point(self):
        """カーソル位置を返す (JavaFuture[int])。"""
        return wrap(self._win.point())

    def goto_char(self, position):
        """カーソルを移動する (JavaFuture[None])。"""
        return wrap(self._win.gotoChar(position))

    def insert(self, text):
        """テキストを挿入する (JavaFuture[None])。"""
        return wrap(self._win.insert(text))

    def delete_backward(self, count):
        """前方にcount文字削除する (JavaFuture[None])。"""
        return wrap(self._win.deleteBackward(count))

    def delete_forward(self, count):
        """後方にcount文字削除する (JavaFuture[None])。"""
        return wrap(self._win.deleteForward(count))

    def buffer(self):
        """このウィンドウのバッファを返す (JavaFuture[Buffer])。"""
        from alle.buffer import Buffer
        return wrap_transform(self._win.buffer(), lambda b: Buffer(b))
