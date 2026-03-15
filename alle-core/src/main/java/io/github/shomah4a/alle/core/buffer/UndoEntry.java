package io.github.shomah4a.alle.core.buffer;

/**
 * Undo/Redo用の変更記録。テキスト変更とカーソル位置をペアで保持する。
 *
 * @param change テキスト変更操作
 * @param cursorPosition 変更前のカーソル位置
 */
public record UndoEntry(TextChange change, int cursorPosition) {}
