package io.github.shomah4a.alle.core.command.commands;

/**
 * 矩形範囲。
 * [startLine, endLine] の各行に対し、表示カラム範囲 [leftCol, rightCol) を示す。
 * leftCol == rightCol の場合は幅 0（行単位の操作のみ）を意味する。
 */
public record Rectangle(int startLine, int endLine, int leftCol, int rightCol) {

    public Rectangle {
        if (startLine < 0 || endLine < startLine) {
            throw new IllegalArgumentException(
                    "invalid line range: startLine=" + startLine + " endLine=" + endLine);
        }
        if (leftCol < 0 || rightCol < leftCol) {
            throw new IllegalArgumentException(
                    "invalid column range: leftCol=" + leftCol + " rightCol=" + rightCol);
        }
    }

    public int width() {
        return rightCol - leftCol;
    }

    public int lineCount() {
        return endLine - startLine + 1;
    }
}
