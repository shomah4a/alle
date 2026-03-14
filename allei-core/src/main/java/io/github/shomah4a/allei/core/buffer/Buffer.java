package io.github.shomah4a.allei.core.buffer;

import io.github.shomah4a.allei.core.textmodel.TextModel;
import java.nio.file.Path;
import java.util.Optional;

/**
 * エディタのバッファ。
 * {@link TextModel}を保持し、ファイルパス、変更フラグを管理する。
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

    public void markDirty() {
        this.dirty = true;
    }

    public void markClean() {
        this.dirty = false;
    }

    /**
     * 全テキストを返す。
     */
    public String getText() {
        return textModel.getText();
    }
}
