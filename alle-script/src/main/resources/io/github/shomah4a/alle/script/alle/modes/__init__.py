"""組み込みモードの登録。

エディタ初期化時に register_modes() が呼ばれ、
各モードを ModeRegistry と AutoModeMap に登録する。
"""

from __future__ import annotations

import alle
from alle.modes.electric_pair import ElectricPairMode
from alle.modes.python import PythonMode


def register_modes() -> None:
    """組み込みモードを一括登録する。"""
    alle.register_major_mode(
        PythonMode,
        extensions=["py", "pyw"],
        shebangs=["python", "python2", "python3"],
    )
    alle.register_minor_mode(ElectricPairMode)
