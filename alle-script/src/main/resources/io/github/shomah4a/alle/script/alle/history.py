"""入力履歴のPythonラッパー。"""

from __future__ import annotations

from typing import Any

import java  # type: ignore[import-untyped]


class InputHistory:
    """Java側のInputHistoryをラップする入力履歴。

    スクリプト側で履歴を明示的に管理するために使用する。
    モジュールレベルの変数として保持し、 ``alle.prompt()`` に渡すことで
    同一コマンドの入力履歴を維持できる。

    :param max_size: 履歴の最大保持件数（デフォルト: 100）
    :type max_size: int | None

    使用例::

        from alle.history import InputHistory
        history = InputHistory()
        alle.prompt("Input: ", history=history)
    """

    def __init__(self, max_size: int | None = None) -> None:
        InputHistoryClass = java.type(
            "io.github.shomah4a.alle.core.input.InputHistory"
        )
        if max_size is not None:
            self._java_history: Any = InputHistoryClass(max_size)
        else:
            self._java_history: Any = InputHistoryClass()

    @property
    def java_history(self) -> Any:
        """Java側のInputHistoryインスタンスを返す。

        :return: Java InputHistory
        """
        return self._java_history

    def size(self) -> int:
        """履歴のサイズを返す。

        :return: 履歴件数
        :rtype: int
        """
        return int(self._java_history.size())
