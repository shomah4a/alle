"""プロンプト関連のJava連携ヘルパー（内部用）。"""

from __future__ import annotations

import concurrent.futures
import logging
from typing import Any, Callable

from alle.prompt import Cancelled, Confirmed

logger = logging.getLogger(__name__)


def _wrap_prompt_result(java_result: Any) -> Confirmed | Cancelled:
    """Java側のPromptResultをPython側のクラスに変換する。

    :param java_result: Java側のPromptResult（Confirmed or Cancelled）
    :return: Python側のConfirmed or Cancelled
    """
    class_name = java_result.getClass().getSimpleName()
    if class_name == "Confirmed":
        return Confirmed(str(java_result.value()))
    return Cancelled()


class PromptFuture(concurrent.futures.Future):
    """CompletableFuture<PromptResult> のPythonラッパー。

    ``concurrent.futures.Future[Confirmed | Cancelled]`` 互換のインターフェースを提供する。
    コマンドの ``run()`` 内では ``add_done_callback()`` や ``on_confirmed()`` で
    後続処理を登録し、ブロッキングせずに返すこと。

    :param java_future: Java側のCompletableFuture<PromptResult>
    :type java_future: Any
    """

    def __init__(self, java_future: Any) -> None:
        super().__init__()
        self._java_future: Any = java_future
        java_future.whenComplete(self._on_java_complete)

    def _on_java_complete(self, java_result: Any, exception: Any) -> None:
        if exception is not None:
            self.set_exception(RuntimeError(str(exception.getMessage())))
        else:
            self.set_result(_wrap_prompt_result(java_result))

    def on_confirmed(self, fn: Callable[[str], Any]) -> PromptFuture:
        """入力が確定された場合のみコールバックを実行する。

        :param fn: 確定値（str）を受け取るコールバック
        :type fn: Callable[[str], Any]
        :return: self（チェイン用）
        :rtype: PromptFuture
        """
        def callback(future: concurrent.futures.Future) -> None:
            exc = future.exception()
            if exc is not None:
                logger.warning("プロンプトが例外で完了: %s", exc)
                return
            r = future.result()
            if r.is_confirmed():
                fn(r.value)

        self.add_done_callback(callback)
        return self

    def on_cancelled(self, fn: Callable[[], Any]) -> PromptFuture:
        """入力がキャンセルされた場合のみコールバックを実行する。

        :param fn: 引数なしのコールバック
        :type fn: Callable[[], Any]
        :return: self（チェイン用）
        :rtype: PromptFuture
        """
        def callback(future: concurrent.futures.Future) -> None:
            exc = future.exception()
            if exc is not None:
                logger.warning("プロンプトが例外で完了: %s", exc)
                return
            r = future.result()
            if r.is_cancelled():
                fn()

        self.add_done_callback(callback)
        return self
