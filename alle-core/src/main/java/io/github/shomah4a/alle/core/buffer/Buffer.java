package io.github.shomah4a.alle.core.buffer;

import io.github.shomah4a.alle.core.textmodel.TextModel;
import java.nio.file.Path;
import java.util.Optional;

/**
 * エディタのバッファ。
 * テキストデータ、ファイルパス、変更フラグを管理する。
 * カーソル位置等のビュー固有の状態はWindowが持つ。
 */
public class Buffer {

    private final String name;
    private final TextModel textModel;
    private Path filePath;
    private boolean dirty;

    public Buffer(String name, TextModel textModel) {
        this.name = name;
        this.textModel = textModel;
        this.dirty = false;
    }

    public Buffer(String name, TextModel textModel, Path filePath) {
        this(name, textModel);
        this.filePath = filePath;
    }

    public String getName() {
        return name;
    }

    public Optional<Path> getFilePath() {
        return Optional.ofNullable(filePath);
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void markClean() {
        this.dirty = false;
    }

    /**
     * テキストの長さをコードポイント数で返す。
     */
    public int length() {
        return textModel.length();
    }

    /**
     * 指定位置のコードポイントを返す。
     *
     * @throws IndexOutOfBoundsException indexが範囲外の場合
     */
    public int codePointAt(int index) {
        return textModel.codePointAt(index);
    }

    /**
     * 指定位置に文字列を挿入する。
     *
     * @throws IndexOutOfBoundsException indexが範囲外の場合
     */
    public void insertText(int index, String text) {
        textModel.insert(index, text);
    }

    /**
     * 指定位置から指定コードポイント数を削除する。
     *
     * @throws IndexOutOfBoundsException 範囲が不正な場合
     */
    public void deleteText(int index, int count) {
        textModel.delete(index, count);
    }

    /**
     * 指定範囲の部分文字列を返す。
     *
     * @throws IndexOutOfBoundsException 範囲が不正な場合
     */
    public String substring(int start, int end) {
        return textModel.substring(start, end);
    }

    /**
     * 行数を返す。
     */
    public int lineCount() {
        return textModel.lineCount();
    }

    /**
     * 指定行の先頭オフセットをコードポイント単位で返す。
     *
     * @throws IndexOutOfBoundsException lineIndexが範囲外の場合
     */
    public int lineStartOffset(int lineIndex) {
        return textModel.lineStartOffset(lineIndex);
    }

    /**
     * 指定行のテキストを返す(改行文字を含まない)。
     *
     * @throws IndexOutOfBoundsException lineIndexが範囲外の場合
     */
    public String lineText(int lineIndex) {
        return textModel.lineText(lineIndex);
    }

    /**
     * 全テキストを返す。
     */
    public String getText() {
        return textModel.getText();
    }
}
