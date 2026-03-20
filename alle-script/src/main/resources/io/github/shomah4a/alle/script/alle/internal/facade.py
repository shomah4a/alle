"""EditorFacade の管理と内部ヘルパー。"""

from __future__ import annotations

from typing import Any

from alle.command import CommandBase
from alle.internal.command import make_command
from alle.internal.mode import make_major_mode, make_minor_mode
from alle.mode import MajorModeBase, MinorModeBase

_editor_facade: Any | None = None


def _init(editor_facade: Any) -> None:
    """エンジン初期化時に呼ばれる。"""
    global _editor_facade
    _editor_facade = editor_facade


def _wrap_command(command: CommandBase) -> Any:
    """CommandBase を Java Command にラップする。"""
    return make_command(command)


def _wrap_major_mode(mode: MajorModeBase) -> Any:
    """MajorModeBase を Java MajorMode にラップする。"""
    return make_major_mode(mode)


def _wrap_minor_mode(mode: MinorModeBase) -> Any:
    """MinorModeBase を Java MinorMode にラップする。"""
    return make_minor_mode(mode)


def _require_facade() -> Any:
    if _editor_facade is None:
        raise RuntimeError("alle module is not initialized")
    return _editor_facade
