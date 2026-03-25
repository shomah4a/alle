package io.github.shomah4a.alle.script;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.Nullable;

/**
 * MessageBufferに出力するOutputStream。
 * 書き込まれたバイト列をUTF-8デコードし、改行ごとにMessageBuffer.message()で追加する。
 * GraalVM Context.Builder の .out() / .err() / .logHandler() で使用する。
 *
 * <p>バッファは遅延初期化で作成し、BufferManagerに登録する。
 */
public class MessageBufferOutputStream extends OutputStream {

    private final BufferManager bufferManager;
    private final String bufferName;
    private final int maxLines;
    private final ByteArrayOutputStream pending;
    private final CharsetDecoder decoder;
    private @Nullable MessageBuffer messageBuffer;

    public MessageBufferOutputStream(BufferManager bufferManager, String bufferName, int maxLines) {
        this.bufferManager = bufferManager;
        this.bufferName = bufferName;
        this.maxLines = maxLines;
        this.pending = new ByteArrayOutputStream();
        this.decoder = StandardCharsets.UTF_8.newDecoder();
    }

    @Override
    public void write(int b) {
        pending.write(b);
        if (b == '\n') {
            flushLine();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        for (int i = off; i < off + len; i++) {
            pending.write(b[i]);
            if (b[i] == '\n') {
                flushLine();
            }
        }
    }

    @Override
    public void flush() {
        if (pending.size() > 0) {
            flushLine();
        }
    }

    @Override
    public void close() {
        flush();
    }

    private MessageBuffer getOrCreateBuffer() {
        if (messageBuffer != null && bufferManager.findByName(bufferName).isPresent()) {
            return messageBuffer;
        }
        messageBuffer = new MessageBuffer(bufferName, maxLines);
        bufferManager.add(new BufferFacade(messageBuffer));
        return messageBuffer;
    }

    private void flushLine() {
        byte[] bytes = pending.toByteArray();
        pending.reset();

        ByteBuffer input = ByteBuffer.wrap(bytes);
        CharBuffer output = CharBuffer.allocate(bytes.length * 2);
        decoder.reset();
        decoder.decode(input, output, true);
        decoder.flush(output);
        output.flip();

        String line = output.toString();
        // 末尾の改行を除去
        if (line.endsWith("\n")) {
            line = line.substring(0, line.length() - 1);
        }
        // \r\n の場合も対応
        if (line.endsWith("\r")) {
            line = line.substring(0, line.length() - 1);
        }

        if (!line.isEmpty()) {
            getOrCreateBuffer().message(line);
        }
    }
}
