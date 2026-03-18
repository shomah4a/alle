package io.github.shomah4a.alle.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BufferNameCompleterTest {

    private BufferManager bufferManager;
    private BufferNameCompleter completer;

    @BeforeEach
    void setUp() {
        bufferManager = new BufferManager();
        bufferManager.add(new EditableBuffer("*scratch*", new GapTextModel()));
        bufferManager.add(new EditableBuffer("foo.txt", new GapTextModel()));
        bufferManager.add(new EditableBuffer("foobar.txt", new GapTextModel()));
        bufferManager.add(new EditableBuffer("bar.txt", new GapTextModel()));
        completer = new BufferNameCompleter(bufferManager);
    }

    @Test
    void 前方一致する候補を返す() {
        var result = completer.complete("foo");
        assertEquals(2, result.size());
        assertTrue(result.contains("foo.txt"));
        assertTrue(result.contains("foobar.txt"));
    }

    @Test
    void 一致する候補がない場合は空リストを返す() {
        var result = completer.complete("xyz");
        assertTrue(result.isEmpty());
    }

    @Test
    void 空文字列は全バッファを返す() {
        var result = completer.complete("");
        assertEquals(4, result.size());
    }

    @Test
    void 完全一致する候補を返す() {
        var result = completer.complete("foo.txt");
        assertEquals(1, result.size());
        assertEquals("foo.txt", result.get(0));
    }
}
