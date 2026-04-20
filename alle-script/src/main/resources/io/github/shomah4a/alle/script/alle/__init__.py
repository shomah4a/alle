"""alle - エディタスクリプティングモジュール。

Java側のEditorFacadeをラップし、Python的なAPIを提供する。

使用例:

>>> import alle
>>> from alle.command import CommandBase
>>> from alle.keybind import ctrl, meta, key
>>> class MyCommand(CommandBase):
...     def name(self):
...         return "my-command"
...     def run(self):
...         alle.message("hello")
>>> cmd = MyCommand()
>>> alle.register_command(cmd)
>>> alle.global_set_key([ctrl('c'), key('h')], cmd)
"""

from __future__ import annotations

import concurrent.futures
from typing import Any

from alle.buffer import Buffer
from alle.command import CommandBase
from alle.history import InputHistory
from alle.internal.facade import (
    _init,
    _make_major_mode_factory,
    _make_minor_mode_factory,
    _require_facade,
    _wrap_command,
)
from alle.internal.prompt import PromptFuture
from alle.mode import MajorModeBase, MinorModeBase
from alle.prompt import Cancelled, Confirmed
from alle.window import Window


def active_window() -> Window:
    """アクティブウィンドウを返す。

    :return: アクティブな Window
    :rtype: Window
    """
    return Window(_require_facade().activeWindow())


def current_buffer() -> Buffer:
    """カレントバッファを返す。

    :return: カレントバッファ
    :rtype: Buffer
    """
    return Buffer(_require_facade().currentBuffer())


def message(text: str) -> None:
    """エコーエリアにメッセージを表示する。

    :param text: 表示するメッセージ文字列
    :type text: str
    """
    _require_facade().message(text)


def register_command(command: CommandBase) -> None:
    """コマンドを登録する。同名のコマンドが既に存在する場合は上書きする。

    :param command: CommandBase のサブクラスのインスタンス
    :type command: CommandBase
    """
    _require_facade().registerCommand(_wrap_command(command))


def global_set_key(keystrokes: list[Any], command: CommandBase) -> None:
    """グローバルキーマップにキーバインドを設定する。

    :param keystrokes: KeyStroke のリスト（例: ``[ctrl('x'), ctrl('f')]``）
    :type keystrokes: list[Any]
    :param command: CommandBase のサブクラスのインスタンス
    :type command: CommandBase
    """
    _require_facade().globalSetKey(keystrokes, _wrap_command(command))


def register_major_mode(
    mode_class: type[MajorModeBase],
    *,
    extensions: list[str] | None = None,
    shebangs: list[str] | None = None,
) -> None:
    """メジャーモードを登録する。同名のモードが既に存在する場合は上書きする。

    :param mode_class: MajorModeBase のサブクラス（クラスそのもの）
    :type mode_class: type[MajorModeBase]
    :param extensions: 関連付けるファイル拡張子のリスト（ドットなし）。
        指定した場合、拡張子→モード名のマッピングも自動登録する。
    :type extensions: list[str] | None
    :param shebangs: 関連付ける shebang インタプリタ名のリスト（basename、例: "python3"）。
        指定した場合、shebang→モード名のマッピングも自動登録する。
    :type shebangs: list[str] | None

    使用例:

    >>> from alle.mode import MajorModeBase
    >>> class PythonMode(MajorModeBase):
    ...     def name(self):
    ...         return "python"
    >>> alle.register_major_mode(
    ...     PythonMode,
    ...     extensions=["py", "pyw"],
    ...     shebangs=["python", "python3"],
    ... )
    """
    facade = _require_facade()
    factory = _make_major_mode_factory(mode_class)
    facade.registerMajorMode(factory)
    if extensions or shebangs:
        mode_name = mode_class().name()
        if extensions:
            for ext in extensions:
                facade.registerAutoMode(ext, mode_name)
        if shebangs:
            for command in shebangs:
                facade.registerAutoModeShebang(command, mode_name)


def register_minor_mode(mode_class: type[MinorModeBase]) -> None:
    """マイナーモードを登録する。同名のモードが既に存在する場合は上書きする。

    :param mode_class: MinorModeBase のサブクラス（クラスそのもの）
    :type mode_class: type[MinorModeBase]

    使用例:

    >>> from alle.mode import MinorModeBase
    >>> class AutoSaveMode(MinorModeBase):
    ...     def name(self):
    ...         return "auto-save"
    >>> alle.register_minor_mode(AutoSaveMode)
    """
    facade = _require_facade()
    factory = _make_minor_mode_factory(mode_class)
    facade.registerMinorMode(factory)


def add_major_mode_hook(mode_name: str, hook: callable) -> None:
    """メジャーモード有効化時のフックを追加する。

    フック関数は ``(buffer, mode_name)`` を引数に取る。
    ``buffer`` は ``alle.buffer.Buffer`` インスタンス。

    :param mode_name: フックを紐付けるモード名（例: ``"python"``）
    :type mode_name: str
    :param hook: 有効化時に実行される関数。引数は ``(buffer, mode_name)``
    :type hook: callable

    使用例:

    >>> def setup_python(buffer, mode_name):
    ...     # buffer に対してマイナーモードを有効化する等
    ...     pass
    >>> alle.add_major_mode_hook("python", setup_python)
    """
    def wrapper(java_buffer, mode):
        hook(Buffer(java_buffer), mode)
    _require_facade().addMajorModeHook(mode_name, wrapper)


def add_minor_mode_hook(mode_name: str, hook: callable) -> None:
    """マイナーモード有効化時のフックを追加する。

    フック関数は ``(buffer, mode_name)`` を引数に取る。
    ``buffer`` は ``alle.buffer.Buffer`` インスタンス。

    :param mode_name: フックを紐付けるモード名（例: ``"electric-pair"``）
    :type mode_name: str
    :param hook: 有効化時に実行される関数。引数は ``(buffer, mode_name)``
    :type hook: callable
    """
    def wrapper(java_buffer, mode):
        hook(Buffer(java_buffer), mode)
    _require_facade().addMinorModeHook(mode_name, wrapper)


def save_frame_state(name: str) -> None:
    """現在のフレーム状態を名前付きで保存する。

    :param name: 保存名
    :type name: str
    """
    _require_facade().saveFrameState(name)


def restore_frame_state(name: str) -> bool:
    """保存済みフレーム状態を名前で復元する。

    :param name: 復元対象の保存名
    :type name: str
    :return: 復元に成功した場合True、名前が見つからない場合False
    :rtype: bool
    """
    return _require_facade().restoreFrameState(name)


def has_frame_state(name: str) -> bool:
    """指定名のフレーム状態が保存済みかどうかを返す。

    :param name: 確認対象の保存名
    :type name: str
    :return: 保存済みの場合True
    :rtype: bool
    """
    return _require_facade().hasFrameState(name)


def prompt(
    message: str,
    history: InputHistory | None = None,
    initial_value: str | None = None,
) -> concurrent.futures.Future[Confirmed | Cancelled]:
    """プロンプトを表示してユーザーから文字列入力を受け付ける。

    返却されるFutureはミニバッファで入力が確定またはキャンセルされた時点で完了する。
    コマンドの ``run()`` 内では ``add_done_callback()`` や
    ``on_confirmed()`` / ``on_cancelled()`` で後続処理を登録すること。

    :param message: プロンプトメッセージ
    :type message: str
    :param history: 入力履歴（省略時は新規作成）
    :type history: InputHistory | None
    :param initial_value: 入力エリアの初期値
    :type initial_value: str | None
    :return: 入力結果のFuture（結果はConfirmed or Cancelled）
    :rtype: concurrent.futures.Future[Confirmed | Cancelled]

    使用例::

        from alle.history import InputHistory
        history = InputHistory()

        def run(ctx):
            alle.prompt("Enter name: ", history=history).on_confirmed(
                lambda value: ctx.message(f"Hello, {value}")
            )
    """
    facade = _require_facade()
    java_history = (
        history.java_history if history is not None
        else facade.createInputHistory()
    )
    if initial_value is not None:
        java_future = facade.prompt(message, initial_value, java_history)
    else:
        java_future = facade.prompt(message, java_history)
    return PromptFuture(java_future)


def create_input_history(max_size: int | None = None) -> InputHistory:
    """新しいInputHistoryインスタンスを生成する。

    スクリプト側で入力履歴を明示的に管理するために使用する。

    :param max_size: 履歴の最大保持件数（デフォルト: 100）
    :type max_size: int | None
    :return: 新しいInputHistory
    :rtype: InputHistory
    """
    return InputHistory(max_size=max_size)
