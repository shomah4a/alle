package io.github.shomah4a.alle.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BufferNameCompleterTest {

    private BufferManager bufferManager;
    private BufferNameCompleter completer;

    @BeforeEach
    void setUp() {
        bufferManager = new BufferManager();
        bufferManager.add(new BufferFacade(new TextBuffer("*scratch*", new GapTextModel(), new SettingsRegistry())));
        bufferManager.add(new BufferFacade(new TextBuffer("foo.txt", new GapTextModel(), new SettingsRegistry())));
        bufferManager.add(new BufferFacade(new TextBuffer("foobar.txt", new GapTextModel(), new SettingsRegistry())));
        bufferManager.add(new BufferFacade(new TextBuffer("bar.txt", new GapTextModel(), new SettingsRegistry())));
        completer = new BufferNameCompleter(bufferManager);
    }

    @Test
    void 前方一致する候補を返す() {
        var result = completer.complete("foo");
        assertEquals(2, result.size());
        assertTrue(result.anySatisfy(c -> c.value().equals("foo.txt")));
        assertTrue(result.anySatisfy(c -> c.value().equals("foobar.txt")));
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
        assertEquals("foo.txt", result.get(0).value());
    }

    @Test
    void 候補はすべてterminalである() {
        var result = completer.complete("foo");
        assertTrue(result.allSatisfy(CompletionCandidate::terminal));
    }
}
