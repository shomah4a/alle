"""alle - エディタスクリプティングモジュール。

Java側のEditorFacadeをラップし、Python的なAPIを提供する。

使用例:
    import alle

    win = alle.active_window()
    pos = await win.point()
    await win.insert("hello")

    buf = alle.current_buffer()
    print(buf.name())

    alle.message("done")
"""

from alle.futures import JavaFuture, wrap, wrap_transform
from alle.window import Window
from alle.buffer import Buffer

_editor_facade = None


def _init(editor_facade):
    """エンジン初期化時に呼ばれる。"""
    global _editor_facade
    _editor_facade = editor_facade


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
