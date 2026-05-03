"""エディタ設定値の読み書き API。

EditorSettings に登録されている設定値をスクリプトから操作する。

使用例:

>>> from alle import settings
>>> settings.set_global("completion-ignore-case", True)
>>> settings.get_effective("completion-ignore-case")
True
>>> settings.remove_global("completion-ignore-case")
"""

from __future__ import annotations

from typing import Any

from alle.internal.facade import _require_facade


def set_global(key: str, value: Any) -> None:
    """設定値のグローバル値を設定する。

    値の型は登録済み設定の型と一致する必要がある。型不一致の場合は
    Java 側で ClassCastException が送出される。

    :param key: 設定キー（例: ``"completion-ignore-case"``）
    :type key: str
    :param value: 設定値
    :type value: Any
    :raises Exception: キーが未登録の場合、または値の型が一致しない場合
    """
    _require_facade().setSetting(key, value)


def get_effective(key: str) -> Any:
    """設定値の実効値を取得する。

    グローバル値が未設定なら設定のデフォルト値を返す。

    :param key: 設定キー
    :type key: str
    :return: 実効値
    :rtype: Any
    :raises Exception: キーが未登録の場合
    """
    return _require_facade().getSetting(key)


def remove_global(key: str) -> None:
    """グローバル値を解除し、デフォルト値にフォールバックさせる。

    :param key: 設定キー
    :type key: str
    :raises Exception: キーが未登録の場合
    """
    _require_facade().removeSetting(key)
