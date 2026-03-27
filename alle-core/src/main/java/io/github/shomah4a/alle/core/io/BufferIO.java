package io.github.shomah4a.alle.core.io;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;

/**
 * バッファのファイル読み込み・保存を行う。
 * 実際のIO操作は{@link BufferReader}/{@link BufferWriter}に委譲する。
 */
public class BufferIO {

    private final BufferReader bufferReader;
    private final BufferWriter bufferWriter;
    private final SettingsRegistry settingsRegistry;

    public BufferIO(BufferReader bufferReader, BufferWriter bufferWriter, SettingsRegistry settingsRegistry) {
        this.bufferReader = bufferReader;
        this.bufferWriter = bufferWriter;
        this.settingsRegistry = settingsRegistry;
    }

    /**
     * ファイルを読み込んでバッファを生成する。
     * 改行コードを検出し、内部的にはLFに正規化して保持する。
     *
     * @param filePath 読み込むファイルのパス
     * @return 読み込んだ内容を持つバッファと検出した改行コード
     * @throws IOException 読み込みに失敗した場合
     */
    public LoadResult load(Path filePath) throws IOException {
        String rawText;
        try (Reader reader = bufferReader.open(filePath.toString())) {
            rawText = readAll(reader);
        }

        LineEnding lineEnding = LineEnding.detect(rawText);
        String normalizedText = LineEnding.normalize(rawText);

        String bufferName = filePath.getFileName().toString();
        var buffer = new TextBuffer(bufferName, new GapTextModel(), settingsRegistry, filePath);
        buffer.setLineEnding(lineEnding);
        buffer.getUndoManager().withoutRecording(() -> {
            if (!normalizedText.isEmpty()) {
                buffer.insertText(0, normalizedText);
            }
        });

        var bufferFacade = new BufferFacade(buffer);
        return new LoadResult(bufferFacade, lineEnding);
    }

    /**
     * バッファの内容をファイルに保存する。
     * バッファが保持するLineEndingに変換して書き込む。
     *
     * @param buffer 保存するバッファ
     * @throws IOException 書き込みに失敗した場合
     * @throws IllegalStateException バッファにファイルパスが設定されていない場合
     */
    public void save(BufferFacade buffer) throws IOException {
        Path filePath = buffer.getFilePath()
                .orElseThrow(() -> new IllegalStateException("Buffer has no file path: " + buffer.getName()));

        String text = buffer.getText();
        String denormalizedText = buffer.getLineEnding().denormalize(text);

        try (Writer writer = bufferWriter.open(filePath.toString())) {
            writer.write(denormalizedText);
        }

        buffer.markClean();
    }

    /**
     * 既存バッファの内容をファイルから再読み込みする。
     * バッファのテキストを全て置換し、undo/redo履歴をクリアし、dirtyフラグをリセットする。
     * ファイル読み込みはロック外で行い、バッファ操作はatomicOperationで保護する。
     *
     * @param buffer 再読み込み対象のバッファ（ファイルパスが設定されていること）
     * @throws IOException 読み込みに失敗した場合
     * @throws IllegalStateException バッファにファイルパスが設定されていない場合
     */
    public void reload(BufferFacade buffer) throws IOException {
        Path filePath = buffer.getFilePath()
                .orElseThrow(() -> new IllegalStateException("Buffer has no file path: " + buffer.getName()));

        // ファイル読み込みはロック外で行う
        String rawText;
        try (Reader reader = bufferReader.open(filePath.toString())) {
            rawText = readAll(reader);
        }

        LineEnding lineEnding = LineEnding.detect(rawText);
        String normalizedText = LineEnding.normalize(rawText);

        // バッファ操作はアトミックに行う
        buffer.atomicOperation(bf -> {
            bf.getUndoManager().withoutRecording(() -> {
                int currentLength = bf.length();
                if (currentLength > 0) {
                    bf.deleteText(0, currentLength);
                }
                if (!normalizedText.isEmpty()) {
                    bf.insertText(0, normalizedText);
                }
            });
            bf.setLineEnding(lineEnding);
            bf.getUndoManager().clear();
            bf.markClean();
            return null;
        });
    }

    private static String readAll(Reader reader) throws IOException {
        var sb = new StringBuilder();
        char[] buf = new char[8192];
        int read;
        while ((read = reader.read(buf)) != -1) {
            sb.append(buf, 0, read);
        }
        return sb.toString();
    }

    /**
     * ファイル読み込みの結果。
     */
    public record LoadResult(BufferFacade bufferFacade, LineEnding lineEnding) {}
}
