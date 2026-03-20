"""バッファ操作のPythonラッパー。"""


class Buffer:
    """BufferFacade のラッパー。"""

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
