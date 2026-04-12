package io.github.shomah4a.alle.core.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.modes.text.TextMode;
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
        assertEquals("text", mode.name());
    }

    @Test
    void 拡張子なしのファイルからデフォルトモードが返る() {
        var mode = autoModeMap.resolve("LICENSE");
        assertEquals("text", mode.name());
    }

    @Test
    void ドットで終わるファイルからデフォルトモードが返る() {
        var mode = autoModeMap.resolve("file.");
        assertEquals("text", mode.name());
    }

    @Test
    void 複数ドットのファイル名で最後の拡張子が使われる() {
        autoModeMap.register("gz", StubMode::new);
        var mode = autoModeMap.resolve("archive.tar.gz");
        assertEquals("Stub", mode.name());
    }

    @Test
    void ファイル名完全一致で登録したモードが返る() {
        autoModeMap.registerFileName("Makefile", StubMode::new);
        var mode = autoModeMap.resolve("Makefile");
        assertEquals("Stub", mode.name());
    }

    @Test
    void ファイル名マッチは拡張子マッチより優先される() {
        autoModeMap.register("java", () -> new StubMode("ext"));
        autoModeMap.registerFileName("build.java", () -> new StubMode("filename"));
        var mode = autoModeMap.resolve("build.java");
        assertEquals("filename", mode.name());
    }

    @Test
    void パス付きファイル名でもファイル名マッチが動作する() {
        autoModeMap.registerFileName("Makefile", StubMode::new);
        var mode = autoModeMap.resolve("src/sub/Makefile");
        assertEquals("Stub", mode.name());
    }

    private static class StubMode implements MajorMode {

        private final String modeName;

        StubMode() {
            this("Stub");
        }

        StubMode(String modeName) {
            this.modeName = modeName;
        }

        @Override
        public String name() {
            return modeName;
        }

        @Override
        public Optional<Keymap> keymap() {
            return Optional.empty();
        }
    }
}
