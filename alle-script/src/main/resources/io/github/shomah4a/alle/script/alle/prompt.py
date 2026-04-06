"""プロンプト入力結果の公開クラス。"""

from __future__ import annotations


class Confirmed:
    """ユーザーが入力を確定した結果。

    :param value: 確定された入力値
    :type value: str
    """

    def __init__(self, value: str) -> None:
        self.value: str = value

    def __repr__(self) -> str:
        return f"Confirmed(value={self.value!r})"

    def is_confirmed(self) -> bool:
        return True

    def is_cancelled(self) -> bool:
        return False


class Cancelled:
    """ユーザーが入力をキャンセルした結果。"""

    def __repr__(self) -> str:
        return "Cancelled()"

    def is_confirmed(self) -> bool:
        return False

    def is_cancelled(self) -> bool:
        return True
