"""CompletableFuture のラッピングユーティリティ。

JavaFuture のインスタンス生成を担う内部ヘルパー。
"""

from __future__ import annotations

from typing import Any, Callable, Generator, TypeVar

from alle.futures import JavaFuture

T = TypeVar('T')


def wrap(java_future: Any) -> JavaFuture:
    """CompletableFuture を JavaFuture でラップする。"""
    return JavaFuture(java_future)


def wrap_transform(java_future: Any, transform: Callable[[Any], T]) -> JavaFuture:
    """CompletableFuture の結果を変換してラップする。"""
    class TransformedFuture(JavaFuture):
        def __init__(self, jf: Any, fn: Callable[[Any], T]) -> None:
            super().__init__(jf)
            self._transform: Callable[[Any], T] = fn

        def result(self) -> T:
            raw = super().result()
            return self._transform(raw)

        def __await__(self) -> Generator[JavaFuture, None, T]:
            if not self.done():
                yield self
            return self.result()

    return TransformedFuture(java_future, transform)
