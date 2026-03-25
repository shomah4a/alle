"""EditorFacade の管理と内部ヘルパー。"""

from __future__ import annotations

from typing import Any

from alle.command import CommandBase
from alle.internal.command import make_command
from alle.internal.mode import make_major_mode, make_minor_mode
from alle.mode import MajorModeBase, MinorModeBase

_editor_facade: Any | None = None


def _init(editor_facade: Any) -> None:
    """エンジン初期化時に呼ばれる。

    :param editor_facade: Java 側の EditorFacade インスタンス
    :type editor_facade: io.github.shomah4a.alle.script.EditorFacade
    """
    global _editor_facade
    _editor_facade = editor_facade
    _register_builtin_modes()


def _wrap_command(command: CommandBase) -> Any:
    """CommandBase を Java Command にラップする。

    :param command: ラップ対象のコマンド
    :type command: CommandBase
    :return: Java Command インスタンス
    :rtype: io.github.shomah4a.alle.core.command.Command
    """
    return make_command(command)


def _wrap_major_mode(mode: MajorModeBase) -> Any:
    """MajorModeBase を Java MajorMode にラップする。

    :param mode: ラップ対象のメジャーモード
    :type mode: MajorModeBase
    :return: Java MajorMode インスタンス
    :rtype: io.github.shomah4a.alle.core.mode.MajorMode
    """
    return make_major_mode(mode)


def _wrap_minor_mode(mode: MinorModeBase) -> Any:
    """MinorModeBase を Java MinorMode にラップする。

    :param mode: ラップ対象のマイナーモード
    :type mode: MinorModeBase
    :return: Java MinorMode インスタンス
    :rtype: io.github.shomah4a.alle.core.mode.MinorMode
    """
    return make_minor_mode(mode)


def _make_major_mode_factory(mode_class: type) -> Any:
    """MajorModeBase サブクラスから、呼び出すと Java MajorMode を返すファクトリ関数を作る。

    :param mode_class: MajorModeBase のサブクラス
    :type mode_class: type
    :return: 呼び出すと Java MajorMode を返すファクトリ関数
    :rtype: Callable[[], io.github.shomah4a.alle.core.mode.MajorMode]
    """
    def factory():
        return make_major_mode(mode_class())
    return factory


def _make_minor_mode_factory(mode_class: type) -> Any:
    """MinorModeBase サブクラスから、呼び出すと Java MinorMode を返すファクトリ関数を作る。

    :param mode_class: MinorModeBase のサブクラス
    :type mode_class: type
    :return: 呼び出すと Java MinorMode を返すファクトリ関数
    :rtype: Callable[[], io.github.shomah4a.alle.core.mode.MinorMode]
    """
    def factory():
        return make_minor_mode(mode_class())
    return factory


def _register_builtin_modes() -> None:
    """組み込みモードを登録する。"""
    from alle.modes import register_modes
    register_modes()


def _require_facade() -> Any:
    """初期化済みの EditorFacade を返す。未初期化なら RuntimeError を送出する。

    :return: EditorFacade インスタンス
    :rtype: io.github.shomah4a.alle.script.EditorFacade
    :raises RuntimeError: alle モジュールが未初期化の場合
    """
    if _editor_facade is None:
        raise RuntimeError("alle module is not initialized")
    return _editor_facade
