#!/usr/bin/env python3
"""
ベンチマーク用のキーストロークシーケンスを標準出力に書き出す。
lanterna の UnixTerminal が解釈できるバイト列を生成する。

主にC-p/C-nによるスクロール操作を重点的にテストする。
（大きめのファイルでスクロールが遅い問題の調査用）
"""
import sys


def ctrl(c: str) -> bytes:
    """Control文字を生成する"""
    return bytes([ord(c) - ord('a') + 1])


def write(data: bytes) -> None:
    sys.stdout.buffer.write(data)


def main() -> None:
    iterations = int(sys.argv[1]) if len(sys.argv) > 1 else 500

    # --- フェーズ1: スクロール操作（主要ベンチマーク対象）---
    # 下方向にスクロール
    for _ in range(iterations):
        write(ctrl('n'))  # next-line

    # 上方向にスクロール
    for _ in range(iterations):
        write(ctrl('p'))  # previous-line

    # ページスクロール
    for _ in range(iterations // 10):
        write(ctrl('v'))  # scroll-up (page down)
    for _ in range(iterations // 10):
        # M-v = ESC v (ページアップ)
        write(b'\x1bv')

    # --- フェーズ2: 行内移動 ---
    for _ in range(iterations // 2):
        write(ctrl('a'))  # beginning-of-line
        write(ctrl('e'))  # end-of-line

    # --- フェーズ3: 文字単位の移動 ---
    for _ in range(iterations // 2):
        write(ctrl('f'))  # forward-char
    for _ in range(iterations // 2):
        write(ctrl('b'))  # backward-char

    # --- フェーズ4: テキスト挿入（少量）---
    write(ctrl('a'))
    for i in range(iterations // 10):
        write(f"X{i:03d} ".encode('utf-8'))

    # --- フェーズ5: 削除操作（少量）---
    write(ctrl('a'))
    for _ in range(iterations // 10):
        write(ctrl('d'))  # delete-char

    # --- 終了 ---
    write(ctrl('q'))
    # quit が変更確認を出す場合に備えて
    write(b'yes\n')

    sys.stdout.buffer.flush()


if __name__ == '__main__':
    main()
