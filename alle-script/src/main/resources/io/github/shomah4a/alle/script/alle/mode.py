"""モード基底クラス。

JavaのMajorMode/MinorModeインターフェースを継承し、
Python側でモードを定義するための基底クラスを提供する。

使用例:
    from alle.mode import AlleMajorMode

    class MyMode(AlleMajorMode):
        def name(self):
            return "my-mode"
"""

import java
from java.util import Optional

MajorMode = java.type('io.github.shomah4a.alle.core.mode.MajorMode')
MinorMode = java.type('io.github.shomah4a.alle.core.mode.MinorMode')


class AlleMajorMode(MajorMode):
    """メジャーモードの基底クラス。

    サブクラスは name() を実装する。
    keymap() はデフォルトで空を返す。必要に応じてオーバーライドする。
    """

    def keymap(self):
        return Optional.empty()


class AlleMinorMode(MinorMode):
    """マイナーモードの基底クラス。

    サブクラスは name() を実装する。
    keymap() はデフォルトで空を返す。必要に応じてオーバーライドする。
    """

    def keymap(self):
        return Optional.empty()
