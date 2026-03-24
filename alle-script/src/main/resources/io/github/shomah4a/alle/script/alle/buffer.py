"""バッファ操作のPythonラッパー。"""

from __future__ import annotations

from typing import Any

from alle.futures import JavaFuture
from alle.internal.futures import wrap


class Buffer:
    """BufferFacade のラッパー。"""

    def __init__(self, java_buffer_facade: Any) -> None:
        self._buf: Any = java_buffer_facade

    def name(self) -> JavaFuture:
        """バッファ名を返す (JavaFuture[str])。"""
        return wrap(self._buf.name())

    def text(self) -> JavaFuture:
        """全テキストを返す (JavaFuture[str])。"""
        return wrap(self._buf.text())

    def length(self) -> JavaFuture:
        """テキスト長を返す (JavaFuture[int])。"""
        return wrap(self._buf.length())

    def line_count(self) -> JavaFuture:
        """行数を返す (JavaFuture[int])。"""
        return wrap(self._buf.lineCount())

    def line_text(self, line_index: int) -> JavaFuture:
        """指定行のテキストを返す (JavaFuture[str])。"""
        return wrap(self._buf.lineText(line_index))

    def insert_at(self, index: int, text: str) -> JavaFuture:
        """指定位置にテキストを挿入する (JavaFuture[None])。"""
        return wrap(self._buf.insertAt(index, text))

    def delete_at(self, index: int, count: int) -> JavaFuture:
        """指定位置からcount文字削除する (JavaFuture[None])。"""
        return wrap(self._buf.deleteAt(index, count))

    def substring(self, start: int, end: int) -> JavaFuture:
        """部分文字列を返す (JavaFuture[str])。"""
        return wrap(self._buf.substring(start, end))

    def line_index_for_offset(self, offset: int) -> JavaFuture:
        """オフセットが属する行インデックスを返す (JavaFuture[int])。"""
        return wrap(self._buf.lineIndexForOffset(offset))

    def line_start_offset(self, line_index: int) -> JavaFuture:
        """指定行の先頭オフセットを返す (JavaFuture[int])。"""
        return wrap(self._buf.lineStartOffset(line_index))

    def is_dirty(self) -> JavaFuture:
        """変更済みかどうかを返す (JavaFuture[bool])。"""
        return wrap(self._buf.isDirty())

    def is_read_only(self) -> JavaFuture:
        """読み取り専用かどうかを返す (JavaFuture[bool])。"""
        return wrap(self._buf.isReadOnly())
