"""alle - エディタスクリプティングモジュール。

Java側のEditorFacadeをラップし、Python的なAPIを提供する。
CompletableFutureはJavaFutureでラップされ、awaitで待てる。

使用例:
    import alle

    win = alle.active_window()
    pos = await win.point()
    await win.insert("hello")

    buf = alle.current_buffer()
    print(buf.name())

    alle.message("done")
"""

import asyncio


# --- JavaFuture: asyncio.Future 相当 ---

class JavaFuture:
    """java.util.concurrent.CompletableFuture のラッパー。

    asyncio.Future と同等の API を提供する。
    """

    def __init__(self, java_future):
        self._future = java_future

    def result(self):
        """結果を返す。未完了の場合はブロックする。"""
        try:
            return self._future.join()
        except Exception as e:
            cause = getattr(e, 'getCause', lambda: e)()
            raise RuntimeError(str(cause)) from e

    def done(self):
        """完了済みかどうかを返す。"""
        return self._future.isDone()

    def cancelled(self):
        """キャンセルされたかどうかを返す。"""
        return self._future.isCancelled()

    def exception(self):
        """例外を返す。正常完了の場合はNone。未完了の場合はInvalidStateError。"""
        if not self.done():
            raise asyncio.InvalidStateError('Result is not set.')
        if self._future.isCompletedExceptionally():
            try:
                self._future.join()
            except Exception as e:
                return e
        return None

    def add_done_callback(self, fn):
        """完了時のコールバックを登録する。"""
        def callback(result):
            fn(self)
        self._future.thenAccept(callback)

    def __await__(self):
        if not self.done():
            yield self
        return self.result()

    def __repr__(self):
        if self.done():
            if self._future.isCompletedExceptionally():
                return '<JavaFuture failed>'
            return f'<JavaFuture done: {self._future.join()}>'
        return '<JavaFuture pending>'


def _wrap(java_future):
    """CompletableFuture を JavaFuture でラップする。"""
    return JavaFuture(java_future)


# --- WindowFacade ラッパー ---

class Window:
    """ウィンドウ操作のPythonラッパー。"""

    def __init__(self, java_window_facade):
        self._win = java_window_facade

    def point(self):
        """カーソル位置を返す (JavaFuture[int])。"""
        return _wrap(self._win.point())

    def goto_char(self, position):
        """カーソルを移動する (JavaFuture[None])。"""
        return _wrap(self._win.gotoChar(position))

    def insert(self, text):
        """テキストを挿入する (JavaFuture[None])。"""
        return _wrap(self._win.insert(text))

    def delete_backward(self, count):
        """前方にcount文字削除する (JavaFuture[None])。"""
        return _wrap(self._win.deleteBackward(count))

    def delete_forward(self, count):
        """後方にcount文字削除する (JavaFuture[None])。"""
        return _wrap(self._win.deleteForward(count))

    def buffer(self):
        """このウィンドウのバッファを返す (JavaFuture[Buffer])。"""
        return _wrap_transform(self._win.buffer(), lambda b: Buffer(b))


# JavaFuture に変換ヘルパーを追加
def _wrap_transform(java_future, transform):
    """CompletableFuture の結果を変換してラップする。"""
    class TransformedFuture(JavaFuture):
        def __init__(self, jf, fn):
            super().__init__(jf)
            self._transform = fn

        def result(self):
            raw = super().result()
            return self._transform(raw)

        def __await__(self):
            if not self.done():
                yield self
            return self.result()

    return TransformedFuture(java_future, transform)


# --- BufferFacade ラッパー ---

class Buffer:
    """バッファ操作のPythonラッパー。"""

    def __init__(self, java_buffer_facade):
        self._buf = java_buffer_facade

    def name(self):
        """バッファ名を返す。"""
        return self._buf.name()

    def text(self):
        """全テキストを返す。"""
        return self._buf.text()

    def length(self):
        """テキスト長を返す。"""
        return self._buf.length()

    def line_count(self):
        """行数を返す。"""
        return self._buf.lineCount()

    def line_text(self, line_index):
        """指定行のテキストを返す。"""
        return self._buf.lineText(line_index)

    def insert_at(self, index, text):
        """指定位置にテキストを挿入する。"""
        self._buf.insertAt(index, text)

    def delete_at(self, index, count):
        """指定位置からcount文字削除する。"""
        self._buf.deleteAt(index, count)

    def substring(self, start, end):
        """部分文字列を返す。"""
        return self._buf.substring(start, end)

    def line_index_for_offset(self, offset):
        """オフセットが属する行インデックスを返す。"""
        return self._buf.lineIndexForOffset(offset)

    def line_start_offset(self, line_index):
        """指定行の先頭オフセットを返す。"""
        return self._buf.lineStartOffset(line_index)

    def is_dirty(self):
        """変更済みかどうかを返す。"""
        return self._buf.isDirty()

    def is_read_only(self):
        """読み取り専用かどうかを返す。"""
        return self._buf.isReadOnly()


# --- モジュールレベルAPI ---
# GraalPyEngine初期化時に _editor_facade がバインドされる

_editor_facade = None


def _init(editor_facade):
    """エンジン初期化時に呼ばれる。"""
    global _editor_facade
    _editor_facade = editor_facade


def _require_facade():
    if _editor_facade is None:
        raise RuntimeError("alle module is not initialized")
    return _editor_facade


def active_window():
    """アクティブウィンドウを返す。"""
    return Window(_require_facade().activeWindow())


def current_buffer():
    """カレントバッファを返す。"""
    return Buffer(_require_facade().currentBuffer())


def message(text):
    """エコーエリアにメッセージを表示する。"""
    _require_facade().message(text)
