package io.github.shomah4a.allei.core.buffer;

import io.github.shomah4a.allei.core.textmodel.TextModel;

import java.nio.file.Path;
import java.util.Optional;

/**
 * エディタのバッファ。
 * {@link TextModel}を保持し、ファイルパス、変更フラグ、カーソル位置(point)を管理する。
 */
public class Buffer {

    private final String name;
    private final TextModel textModel;
    private Path filePath;
    private boolean dirty;
    private int point;

    public Buffer(String name, TextModel textModel) {
        this.name = name;
        this.textModel = textModel;
        this.dirty = false;
        this.point = 0;
    }

    public Buffer(String name, TextModel textModel, Path filePath) {
        this(name, textModel);
        this.filePath = filePath;
    }

    public String getName() {
        return name;
    }

    public TextModel getTextModel() {
        return textModel;
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

    public void markClean() {
        this.dirty = false;
    }

    /**
     * カーソル位置(point)をコードポイント単位で返す。
     */
    public int getPoint() {
        return point;
    }

    /**
     * カーソル位置(point)をコードポイント単位で設定する。
     *
     * @throws IndexOutOfBoundsException pointが範囲外の場合
     */
    public void setPoint(int point) {
        if (point < 0 || point > textModel.length()) {
            throw new IndexOutOfBoundsException(
                    "point " + point + " is out of bounds [0, " + textModel.length() + "]");
        }
        this.point = point;
    }

    /**
     * カーソル位置に文字列を挿入し、カーソルを挿入文字列の後ろに移動する。
     */
    public void insert(String text) {
        textModel.insert(point, text);
        int insertedCodePoints = (int) text.codePoints().count();
        point += insertedCodePoints;
        dirty = true;
    }

    /**
     * 指定位置に文字列を挿入する。
     * 挿入位置がカーソル位置以前の場合、カーソル位置を調整する。
     */
    public void insertAt(int offset, String text) {
        textModel.insert(offset, text);
        int insertedCodePoints = (int) text.codePoints().count();
        if (offset <= point) {
            point += insertedCodePoints;
        }
        dirty = true;
    }

    /**
     * 指定位置から指定コードポイント数を削除する。
     * 削除範囲にカーソルが含まれる場合、カーソルを削除開始位置に移動する。
     */
    public void deleteAt(int offset, int count) {
        textModel.delete(offset, count);
        if (point > offset + count) {
            point -= count;
        } else if (point > offset) {
            point = offset;
        }
        dirty = true;
    }

    /**
     * カーソルの手前からcount文字を削除する(バックスペース相当)。
     */
    public void deleteBackward(int count) {
        int deleteCount = Math.min(count, point);
        if (deleteCount == 0) {
            return;
        }
        int deleteStart = point - deleteCount;
        textModel.delete(deleteStart, deleteCount);
        point = deleteStart;
        dirty = true;
    }

    /**
     * カーソルの後ろからcount文字を削除する(Delete相当)。
     */
    public void deleteForward(int count) {
        int remaining = textModel.length() - point;
        int deleteCount = Math.min(count, remaining);
        if (deleteCount == 0) {
            return;
        }
        textModel.delete(point, deleteCount);
        dirty = true;
    }

    /**
     * 全テキストを返す。
     */
    public String getText() {
        return textModel.getText();
    }
}
