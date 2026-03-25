"""Electric Pair マイナーモード。

括弧やクォートの入力時に対応する閉じ文字を自動挿入する。
閉じ文字を入力した時に次の文字が同じ閉じ文字なら挿入せずカーソル移動のみ行う。
"""

from __future__ import annotations

from typing import Any

from alle.keybind import bind_key, create_keymap, key
from alle.mode import MinorModeBase
from alle.modes.electric_pair.commands import (
    close_brace,
    close_bracket,
    close_paren,
    insert_double_quote,
    insert_single_quote,
    open_brace,
    open_bracket,
    open_paren,
)


def _create_electric_pair_keymap() -> Any:
    """Electric Pair モードのキーマップを生成する。

    :return: Keymap インスタンス
    :rtype: Keymap
    """
    keymap = create_keymap("electric-pair")
    bind_key(keymap, key("("), open_paren)
    bind_key(keymap, key(")"), close_paren)
    bind_key(keymap, key("["), open_bracket)
    bind_key(keymap, key("]"), close_bracket)
    bind_key(keymap, key("{"), open_brace)
    bind_key(keymap, key("}"), close_brace)
    bind_key(keymap, key('"'), insert_double_quote)
    bind_key(keymap, key("'"), insert_single_quote)
    return keymap


class ElectricPairMode(MinorModeBase):
    """括弧・クォートの自動ペア挿入マイナーモード。"""

    def __init__(self) -> None:
        self._keymap = _create_electric_pair_keymap()

    def name(self) -> str:
        """モード名を返す。

        :return: "ElectricPair"
        :rtype: str
        """
        return "ElectricPair"

    def keymap(self) -> Any:
        """Electric Pair モードのキーマップを返す。

        :return: Keymap インスタンス
        :rtype: Keymap
        """
        return self._keymap
