package io.github.shomah4a.alle.core.statusline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatusLineRegistryTest {

    @Test
    void 登録したエレメントをlookupで取得できる() {
        var registry = new StatusLineRegistry();
        var slot = new StatusLineSlot("test-slot", ctx -> "hello");
        registry.register(slot);

        var result = registry.lookup("test-slot");
        assertTrue(result.isPresent());
        assertEquals("test-slot", result.get().name());
    }

    @Test
    void 未登録の名前はemptyを返す() {
        var registry = new StatusLineRegistry();

        assertTrue(registry.lookup("nonexistent").isEmpty());
    }

    @Test
    void 同名エレメントの二重登録でIllegalStateExceptionをスローする() {
        var registry = new StatusLineRegistry();
        registry.register(new StatusLineSlot("dup", ctx -> ""));

        assertThrows(IllegalStateException.class, () -> {
            registry.register(new StatusLineSlot("dup", ctx -> ""));
        });
    }

    @Test
    void registeredNames_で登録済み名前の一覧を返す() {
        var registry = new StatusLineRegistry();
        registry.register(new StatusLineSlot("a", ctx -> ""));
        registry.register(new StatusLineSlot("b", ctx -> ""));

        var names = registry.registeredNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("a"));
        assertTrue(names.contains("b"));
    }

    @Test
    void グループも登録できる() {
        var registry = new StatusLineRegistry();
        var group = new StatusLineGroup("my-group");
        registry.register(group);

        var result = registry.lookup("my-group");
        assertTrue(result.isPresent());
        assertEquals("my-group", result.get().name());
    }
}
