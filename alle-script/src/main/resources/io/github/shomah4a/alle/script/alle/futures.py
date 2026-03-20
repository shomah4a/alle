"""CompletableFuture のラッパー。

asyncio.Future 相当の API を提供し、await にも対応する。
"""

import asyncio


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


def wrap(java_future):
    """CompletableFuture を JavaFuture でラップする。"""
    return JavaFuture(java_future)


def wrap_transform(java_future, transform):
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
