"""EditorFacade の管理と内部ヘルパー。"""

from __future__ import annotations

from typing import Any

from alle.command import CommandBase
from alle.internal.command import make_command

_editor_facade: Any | None = None


def _init(editor_facade: Any) -> None:
    """エンジン初期化時に呼ばれる。"""
    global _editor_facade
    _editor_facade = editor_facade


def _wrap_command(command: CommandBase) -> Any:
    """CommandBase を Java Command にラップする。"""
    return make_command(command)


def _require_facade() -> Any:
    if _editor_facade is None:
        raise RuntimeError("alle module is not initialized")
    return _editor_facade
