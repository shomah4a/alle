package io.github.shomah4a.alle.core.io;

import io.github.shomah4a.alle.core.buffer.Buffer;
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

    public BufferIO(BufferReader bufferReader, BufferWriter bufferWriter) {
        this.bufferReader = bufferReader;
        this.bufferWriter = bufferWriter;
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
        var buffer = new Buffer(bufferName, new GapTextModel(), filePath);
        if (!normalizedText.isEmpty()) {
            buffer.insertText(0, normalizedText);
        }

        return new LoadResult(buffer, lineEnding);
    }

    /**
     * バッファの内容をファイルに保存する。
     * 指定された改行コードに変換して書き込む。
     *
     * @param buffer    保存するバッファ
     * @param lineEnding 書き込み時の改行コード
     * @throws IOException 書き込みに失敗した場合
     * @throws IllegalStateException バッファにファイルパスが設定されていない場合
     */
    public void save(Buffer buffer, LineEnding lineEnding) throws IOException {
        Path filePath = buffer.getFilePath()
                .orElseThrow(() -> new IllegalStateException("Buffer has no file path: " + buffer.getName()));

        String text = buffer.getText();
        String denormalizedText = lineEnding.denormalize(text);

        try (Writer writer = bufferWriter.open(filePath.toString())) {
            writer.write(denormalizedText);
        }

        buffer.markClean();
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
    public record LoadResult(Buffer buffer, LineEnding lineEnding) {}
}
