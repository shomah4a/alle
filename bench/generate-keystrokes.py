#!/usr/bin/env python3
"""
ベンチマーク用のキーストロークシーケンスを標準出力に書き出す。
lanterna の UnixTerminal が解釈できるバイト列を生成する。

使い方:
    generate-keystrokes.py [--mode default|split|largefile] [iterations]

モード:
    default   既存シナリオ。スクロール・行内移動・編集の混合
    split     画面分割（C-x 2 / C-x 3 / C-x o）を含む混合シナリオ
    largefile 大ファイル向け。ページスクロール中心、終端まで移動する
"""
import argparse
import sys


def ctrl(c: str) -> bytes:
    """Control文字を生成する"""
    return bytes([ord(c) - ord('a') + 1])


def meta(c: str) -> bytes:
    """Meta(Alt)修飾。ESC + 文字 として送出する"""
    return b'\x1b' + c.encode('utf-8')


CTRL_X = ctrl('x')


def split_below() -> bytes:
    return CTRL_X + b'2'


def split_right() -> bytes:
    return CTRL_X + b'3'


def other_window() -> bytes:
    return CTRL_X + b'o'


def delete_other_windows() -> bytes:
    return CTRL_X + b'1'


def write(data: bytes) -> None:
    sys.stdout.buffer.write(data)


def scenario_default(iterations: int) -> None:
    # --- フェーズ1: スクロール操作 ---
    for _ in range(iterations):
        write(ctrl('n'))  # next-line
    for _ in range(iterations):
        write(ctrl('p'))  # previous-line

    # ページスクロール
    for _ in range(iterations // 10):
        write(ctrl('v'))  # scroll-up (page down)
    for _ in range(iterations // 10):
        write(meta('v'))  # M-v = ESC v (ページアップ)

    # --- フェーズ2: 行内移動 ---
    for _ in range(iterations // 2):
        write(ctrl('a'))
        write(ctrl('e'))

    # --- フェーズ3: 文字単位の移動 ---
    for _ in range(iterations // 2):
        write(ctrl('f'))
    for _ in range(iterations // 2):
        write(ctrl('b'))

    # --- フェーズ4: テキスト挿入（少量）---
    write(ctrl('a'))
    for i in range(iterations // 10):
        write(f"X{i:03d} ".encode('utf-8'))

    # --- フェーズ5: 削除操作（少量）---
    write(ctrl('a'))
    for _ in range(iterations // 10):
        write(ctrl('d'))


def scenario_split(iterations: int) -> None:
    """画面分割シナリオ。

    縦分割→横分割と組み合わせて4ペインを作り、ペイン間移動 + 各ペインで
    スクロールするパターンを繰り返す。各サイクルの末尾でC-x 1により
    単一ウィンドウに戻し、最終的に単一ウィンドウの状態で quit する。
    """
    page_per_pane = max(iterations // 40, 1)
    line_per_pane = max(iterations // 20, 1)
    cycles = max(iterations // 50, 3)

    for _ in range(cycles):
        # 縦に分割（上下2ペイン）
        write(split_below())
        # 横にも分割し4ペインを構成
        write(split_right())
        write(other_window())
        write(split_right())

        # 4ペインを巡回してスクロール
        for _ in range(4):
            for _ in range(line_per_pane):
                write(ctrl('n'))
            for _ in range(page_per_pane):
                write(ctrl('v'))
            for _ in range(line_per_pane):
                write(ctrl('p'))
            for _ in range(page_per_pane):
                write(meta('v'))
            write(other_window())

        # 単一ウィンドウに戻す
        write(delete_other_windows())

        # 単一ウィンドウでも少し動かす（分割直前/直後の差分計測用）
        for _ in range(line_per_pane):
            write(ctrl('n'))
        for _ in range(line_per_pane):
            write(ctrl('p'))


def scenario_largefile(iterations: int) -> None:
    """大ファイル向けシナリオ。

    ページ単位スクロールを中心に、行単位スクロール・行頭/行末移動を組み合わせる。
    M-> / M-< (beginning/end-of-buffer) は未バインドのため、ページスクロールの
    繰り返しでファイル末尾近くまで到達させる。
    """
    pages = max(iterations, 100)
    lines_between = max(iterations // 20, 5)

    # 末尾近くまでページダウン
    for _ in range(pages):
        write(ctrl('v'))

    # 一定間隔で行スクロール
    for _ in range(lines_between):
        write(ctrl('n'))
    for _ in range(lines_between):
        write(ctrl('p'))

    # 先頭近くまでページアップ
    for _ in range(pages):
        write(meta('v'))

    # 行頭/行末移動の繰り返し
    for _ in range(iterations // 2):
        write(ctrl('e'))
        write(ctrl('a'))

    # 再度末尾までページダウン
    for _ in range(pages):
        write(ctrl('v'))


SCENARIOS = {
    "default": scenario_default,
    "split": scenario_split,
    "largefile": scenario_largefile,
}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--mode",
        choices=sorted(SCENARIOS.keys()),
        default="default",
    )
    parser.add_argument("iterations", type=int, nargs="?", default=500)
    args = parser.parse_args()

    SCENARIOS[args.mode](args.iterations)

    # --- 終了 ---
    write(ctrl('q'))
    # quit が変更確認を出す場合に備えて
    write(b'yes\n')

    sys.stdout.buffer.flush()


if __name__ == '__main__':
    main()
