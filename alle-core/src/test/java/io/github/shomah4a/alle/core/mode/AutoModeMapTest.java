package io.github.shomah4a.alle.core.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.keybind.Keymap;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AutoModeMapTest {

    private AutoModeMap autoModeMap;

    @BeforeEach
    void setUp() {
        autoModeMap = new AutoModeMap(TextMode::new);
    }

    @Test
    void 登録済み拡張子のファイルから対応するモードが返る() {
        autoModeMap.register("java", StubMode::new);
        var mode = autoModeMap.resolve("Foo.java");
        assertEquals("Stub", mode.name());
    }

    @Test
    void 未登録拡張子のファイルからデフォルトモードが返る() {
        var mode = autoModeMap.resolve("readme.txt");
        assertEquals("Text", mode.name());
    }

    @Test
    void 拡張子なしのファイルからデフォルトモードが返る() {
        var mode = autoModeMap.resolve("Makefile");
        assertEquals("Text", mode.name());
    }

    @Test
    void ドットで終わるファイルからデフォルトモードが返る() {
        var mode = autoModeMap.resolve("file.");
        assertEquals("Text", mode.name());
    }

    @Test
    void 複数ドットのファイル名で最後の拡張子が使われる() {
        autoModeMap.register("gz", StubMode::new);
        var mode = autoModeMap.resolve("archive.tar.gz");
        assertEquals("Stub", mode.name());
    }

    private static class StubMode implements MajorMode {

        @Override
        public String name() {
            return "Stub";
        }

        @Override
        public Optional<Keymap> keymap() {
            return Optional.empty();
        }
    }
}
