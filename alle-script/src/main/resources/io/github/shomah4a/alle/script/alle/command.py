"""コマンド基底クラス。

Javaの Command インターフェースを継承し、
CompletableFuture のラップを自動化する。
"""

import java
from java.util.concurrent import CompletableFuture

Command = java.type('io.github.shomah4a.alle.core.command.Command')


class AlleCommand(Command):
    """コマンドの基底クラス。

    サブクラスは name() と run() を実装する。
    execute() は run() を呼び出し、CompletableFuture でラップする。

    使用例:
        class MyCommand(AlleCommand):
            def name(self):
                return "my-command"

            def run(self):
                alle.message("hello")
    """

    def execute(self, ctx):
        try:
            self.run()
            return CompletableFuture.completedFuture(None)
        except Exception as e:
            return CompletableFuture.failedFuture(e)

    def run(self):
        """コマンドの実行内容。サブクラスでオーバーライドする。"""
        raise NotImplementedError
