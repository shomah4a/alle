"""alle - エディタスクリプティングモジュール。

Java側のEditorFacadeをラップし、Python的なAPIを提供する。

使用例:
    import alle
    from alle.command import CommandBase
    from alle.keybind import ctrl, meta, key

    class MyCommand(CommandBase):
        def name(self):
            return "my-command"
        def run(self):
            alle.message("hello")

    cmd = MyCommand()
    alle.register_command(cmd)
    alle.global_set_key([ctrl('c'), key('h')], cmd)
"""

from alle.futures import JavaFuture, wrap, wrap_transform
from alle.window import Window
from alle.buffer import Buffer
from alle.command import CommandBase, make_command

_editor_facade = None


def _init(editor_facade):
    """エンジン初期化時に呼ばれる。"""
    global _editor_facade
    _editor_facade = editor_facade


def _wrap_command(command):
    """CommandBase を Java Command にラップする。"""
    return make_command(command)


def _require_facade():
    if _editor_facade is None:
        raise RuntimeError("alle module is not initialized")
    return _editor_facade


def active_window():
    """アクティブウィンドウを返す。"""
    return Window(_require_facade().activeWindow())


def current_buffer():
    """カレントバッファを返す。"""
    return Buffer(_require_facade().currentBuffer())


def message(text):
    """エコーエリアにメッセージを表示する。"""
    _require_facade().message(text)


def register_command(command):
    """コマンドを登録する。同名のコマンドが既に存在する場合は上書きする。

    Args:
        command: CommandBase のサブクラスのインスタンス
    """
    _require_facade().registerCommand(_wrap_command(command))


def global_set_key(keystrokes, command):
    """グローバルキーマップにキーバインドを設定する。

    Args:
        keystrokes: KeyStroke のリスト（例: [ctrl('x'), ctrl('f')]）
        command: CommandBase のサブクラスのインスタンス
    """
    _require_facade().globalSetKey(keystrokes, _wrap_command(command))
