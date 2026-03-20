"""CompletableFuture のラッパー。

asyncio.Future 相当の API を提供し、await にも対応する。
"""

from __future__ import annotations

import asyncio
from typing import Any, Callable, Generator


class JavaFuture:
    """java.util.concurrent.CompletableFuture のラッパー。

    asyncio.Future と同等の API を提供する。
    """

    def __init__(self, java_future: Any) -> None:
        self._future: Any = java_future

    def result(self) -> Any:
        """結果を返す。未完了の場合はブロックする。"""
        try:
            return self._future.join()
        except Exception as e:
            cause = getattr(e, 'getCause', lambda: e)()
            raise RuntimeError(str(cause)) from e

    def done(self) -> bool:
        """完了済みかどうかを返す。"""
        return self._future.isDone()

    def cancelled(self) -> bool:
        """キャンセルされたかどうかを返す。"""
        return self._future.isCancelled()

    def exception(self) -> Exception | None:
        """例外を返す。正常完了の場合はNone。未完了の場合はInvalidStateError。"""
        if not self.done():
            raise asyncio.InvalidStateError('Result is not set.')
        if self._future.isCompletedExceptionally():
            try:
                self._future.join()
            except Exception as e:
                return e
        return None

    def add_done_callback(self, fn: Callable[[JavaFuture], None]) -> None:
        """完了時のコールバックを登録する。"""
        def callback(result: Any) -> None:
            fn(self)
        self._future.thenAccept(callback)

    def __await__(self) -> Generator[JavaFuture, None, Any]:
        if not self.done():
            yield self
        return self.result()

    def __repr__(self) -> str:
        if self.done():
            if self._future.isCompletedExceptionally():
                return '<JavaFuture failed>'
            return f'<JavaFuture done: {self._future.join()}>'
        return '<JavaFuture pending>'
