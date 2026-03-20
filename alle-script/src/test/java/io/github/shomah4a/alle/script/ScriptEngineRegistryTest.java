package io.github.shomah4a.alle.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScriptEngineRegistryTest {

    private ScriptEngineRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ScriptEngineRegistry();
    }

    @Test
    void 未登録の言語IDではemptyを返す() {
        Optional<ScriptEngine> engine = registry.create("nonexistent");
        assertFalse(engine.isPresent());
    }

    @Test
    void 登録したファクトリからエンジンを生成できる() {
        registry.register(new StubScriptEngineFactory("test-lang"));
        Optional<ScriptEngine> engine = registry.create("test-lang");
        assertTrue(engine.isPresent());
        assertEquals("test-lang", engine.get().languageId());
    }

    @Test
    void 利用可能な言語IDの一覧を返す() {
        registry.register(new StubScriptEngineFactory("python"));
        registry.register(new StubScriptEngineFactory("js"));
        var languages = registry.availableLanguages();
        assertEquals(2, languages.size());
        assertEquals("js", languages.get(0));
        assertEquals("python", languages.get(1));
    }

    private static class StubScriptEngineFactory implements ScriptEngineFactory {
        private final String languageId;

        StubScriptEngineFactory(String languageId) {
            this.languageId = languageId;
        }

        @Override
        public String languageId() {
            return languageId;
        }

        @Override
        public ScriptEngine create() {
            return new StubScriptEngine(languageId);
        }
    }

    private static class StubScriptEngine implements ScriptEngine {
        private final String languageId;

        StubScriptEngine(String languageId) {
            this.languageId = languageId;
        }

        @Override
        public String languageId() {
            return languageId;
        }

        @Override
        public ScriptResult eval(String code) {
            return new ScriptResult.Success("");
        }

        @Override
        public void bind(String name, Object value) {}

        @Override
        public void close() {}
    }
}
