package io.github.shomah4a.alle.script;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessageBufferOutputStreamTest {

    private MessageBuffer messageBuffer;
    private MessageBufferOutputStream stream;

    @BeforeEach
    void setUp() {
        messageBuffer = new MessageBuffer("*Test*", 100);
        stream = new MessageBufferOutputStream(messageBuffer);
    }

    @Test
    void 改行で1行ずつバッファに追加される() throws IOException {
        stream.write("hello\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("hello", messageBuffer.lineText(0));
    }

    @Test
    void 複数行が個別に追加される() throws IOException {
        stream.write("line1\nline2\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("line1", messageBuffer.lineText(0));
        assertEquals("line2", messageBuffer.lineText(1));
    }

    @Test
    void flushで未送出の内容が送出される() throws IOException {
        stream.write("no newline".getBytes(StandardCharsets.UTF_8));
        assertEquals(1, messageBuffer.lineCount()); // 空バッファは1行
        stream.flush();
        assertEquals("no newline", messageBuffer.lineText(0));
    }

    @Test
    void closeでflushが呼ばれる() throws IOException {
        stream.write("closing".getBytes(StandardCharsets.UTF_8));
        stream.close();
        assertEquals("closing", messageBuffer.lineText(0));
    }

    @Test
    void バイト単位の書き込みで改行を検出する() throws IOException {
        for (byte b : "byte\n".getBytes(StandardCharsets.UTF_8)) {
            stream.write(b);
        }
        assertEquals("byte", messageBuffer.lineText(0));
    }

    @Test
    void マルチバイト文字を正しくデコードする() throws IOException {
        stream.write("日本語テスト\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("日本語テスト", messageBuffer.lineText(0));
    }

    @Test
    void マルチバイト文字のバイト境界分割に対応する() throws IOException {
        byte[] bytes = "あ\n".getBytes(StandardCharsets.UTF_8);
        // 「あ」は3バイト（0xE3 0x81 0x82）、1バイトずつ書き込む
        for (byte b : bytes) {
            stream.write(b);
        }
        assertEquals("あ", messageBuffer.lineText(0));
    }

    @Test
    void CRLF改行を正しく処理する() throws IOException {
        stream.write("windows\r\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("windows", messageBuffer.lineText(0));
    }

    @Test
    void 空行は無視される() throws IOException {
        stream.write("\n".getBytes(StandardCharsets.UTF_8));
        // 空行は message() されない
        assertEquals(1, messageBuffer.lineCount()); // 空バッファの初期状態
    }
}
