"""Python メジャーモード。

.py / .pyw ファイルに適用される。
"""

from __future__ import annotations

from typing import Any

from alle.mode import MajorModeBase
from alle.modes.python.styler import create_python_styler


class PythonMode(MajorModeBase):
    """Python のメジャーモード。"""

    def __init__(self) -> None:
        self._styler = create_python_styler()

    def name(self) -> str:
        """モード名を返す。

        :return: "Python"
        :rtype: str
        """
        return "Python"

    def styler(self) -> Any:
        """Python のシンタックススタイラーを返す。

        :return: RegexStyler インスタンス
        :rtype: RegexStyler
        """
        return self._styler
