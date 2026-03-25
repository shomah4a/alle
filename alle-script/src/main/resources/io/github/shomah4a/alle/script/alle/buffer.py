"""バッファ操作のPythonラッパー。"""

from __future__ import annotations

from typing import Any


class Buffer:
    """BufferFacade のラッパー。

    :param java_buffer_facade: Java 側の BufferFacade インスタンス
    :type java_buffer_facade: Any
    """

    def __init__(self, java_buffer_facade: Any) -> None:
        self._buf: Any = java_buffer_facade

    def name(self) -> str:
        """バッファ名を返す。

        :return: バッファ名
        :rtype: str
        """
        return self._buf.name()

    def text(self) -> str:
        """全テキストを返す。

        :return: バッファの全テキスト
        :rtype: str
        """
        return self._buf.text()

    def length(self) -> int:
        """テキスト長を返す。

        :return: テキストの文字数
        :rtype: int
        """
        return self._buf.length()

    def line_count(self) -> int:
        """行数を返す。

        :return: 行数
        :rtype: int
        """
        return self._buf.lineCount()

    def line_text(self, line_index: int) -> str:
        """指定行のテキストを返す。

        :param line_index: 行インデックス（0始まり）
        :type line_index: int
        :return: 指定行のテキスト
        :rtype: str
        """
        return self._buf.lineText(line_index)

    def insert_at(self, index: int, text: str) -> None:
        """指定位置にテキストを挿入する。

        :param index: 挿入位置のオフセット
        :type index: int
        :param text: 挿入するテキスト
        :type text: str
        """
        self._buf.insertAt(index, text)

    def delete_at(self, index: int, count: int) -> None:
        """指定位置からcount文字削除する。

        :param index: 削除開始位置のオフセット
        :type index: int
        :param count: 削除する文字数
        :type count: int
        """
        self._buf.deleteAt(index, count)

    def substring(self, start: int, end: int) -> str:
        """部分文字列を返す。

        :param start: 開始オフセット（含む）
        :type start: int
        :param end: 終了オフセット（含まない）
        :type end: int
        :return: 部分文字列
        :rtype: str
        """
        return self._buf.substring(start, end)

    def line_index_for_offset(self, offset: int) -> int:
        """オフセットが属する行インデックスを返す。

        :param offset: テキスト内のオフセット
        :type offset: int
        :return: 行インデックス（0始まり）
        :rtype: int
        """
        return self._buf.lineIndexForOffset(offset)

    def line_start_offset(self, line_index: int) -> int:
        """指定行の先頭オフセットを返す。

        :param line_index: 行インデックス（0始まり）
        :type line_index: int
        :return: 行の先頭オフセット
        :rtype: int
        """
        return self._buf.lineStartOffset(line_index)

    def is_dirty(self) -> bool:
        """変更済みかどうかを返す。

        :return: 変更済みなら True
        :rtype: bool
        """
        return self._buf.isDirty()

    def is_read_only(self) -> bool:
        """読み取り専用かどうかを返す。

        :return: 読み取り専用なら True
        :rtype: bool
        """
        return self._buf.isReadOnly()
