package io.github.shomah4a.alle.core.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void shebangのbasenameから登録モードが返る() {
        autoModeMap.registerShebang("bash", () -> new StubMode("bash"));
        var mode = autoModeMap.resolve("myscript", () -> "#!/bin/bash");
        assertEquals("bash", mode.name());
    }

    @Test
    void envの後ろのコマンドからshebangマッチする() {
        autoModeMap.registerShebang("python3", () -> new StubMode("python3"));
        var mode = autoModeMap.resolve("tool", () -> "#!/usr/bin/env python3");
        assertEquals("python3", mode.name());
    }

    @Test
    void envのSオプションをスキップしてコマンドを取得する() {
        autoModeMap.registerShebang("python3", () -> new StubMode("python3"));
        var mode = autoModeMap.resolve("tool", () -> "#!/usr/bin/env -S python3 -u");
        assertEquals("python3", mode.name());
    }

    @Test
    void envの変数設定トークンをスキップしてコマンドを取得する() {
        autoModeMap.registerShebang("python3", () -> new StubMode("python3"));
        var mode = autoModeMap.resolve("tool", () -> "#!/usr/bin/env FOO=bar python3");
        assertEquals("python3", mode.name());
    }

    @Test
    void shebangに引数が付いていてもインタプリタ名が抽出される() {
        autoModeMap.registerShebang("python3", () -> new StubMode("python3"));
        var mode = autoModeMap.resolve("tool", () -> "#!/usr/bin/python3 -u");
        assertEquals("python3", mode.name());
    }

    @Test
    void shebang未登録の場合はデフォルトモードが返る() {
        var mode = autoModeMap.resolve("tool", () -> "#!/bin/bash");
        assertEquals("text", mode.name());
    }

    @Test
    void 先頭行がshebangでない場合はデフォルトモードが返る() {
        autoModeMap.registerShebang("bash", () -> new StubMode("bash"));
        var mode = autoModeMap.resolve("tool", () -> "echo hello");
        assertEquals("text", mode.name());
    }

    @Test
    void 先頭行が空の場合はデフォルトモードが返る() {
        autoModeMap.registerShebang("bash", () -> new StubMode("bash"));
        var mode = autoModeMap.resolve("tool", () -> "");
        assertEquals("text", mode.name());
    }

    @Test
    void 拡張子マッチはshebangマッチより優先される() {
        autoModeMap.register("sh", () -> new StubMode("ext"));
        autoModeMap.registerShebang("python3", () -> new StubMode("python3"));
        var mode = autoModeMap.resolve("tool.sh", () -> "#!/usr/bin/env python3");
        assertEquals("ext", mode.name());
    }

    @Test
    void ファイル名マッチはshebangマッチより優先される() {
        autoModeMap.registerFileName("Makefile", () -> new StubMode("makefile"));
        autoModeMap.registerShebang("python3", () -> new StubMode("python3"));
        var mode = autoModeMap.resolve("Makefile", () -> "#!/usr/bin/env python3");
        assertEquals("makefile", mode.name());
    }

    @Test
    void 単純パースで空文字列は無視される() {
        assertFalse(AutoModeMap.parseShebangCommand("").isPresent());
        assertFalse(AutoModeMap.parseShebangCommand("#").isPresent());
        assertFalse(AutoModeMap.parseShebangCommand("#!").isPresent());
        assertFalse(AutoModeMap.parseShebangCommand("#! ").isPresent());
        assertFalse(AutoModeMap.parseShebangCommand("not a shebang").isPresent());
    }

    @Test
    void 単純パースでパスからbasenameが抽出される() {
        assertTrue(AutoModeMap.parseShebangCommand("#!/bin/bash").isPresent());
        assertEquals("bash", AutoModeMap.parseShebangCommand("#!/bin/bash").get());
        assertEquals(
                "node", AutoModeMap.parseShebangCommand("#!/usr/local/bin/node").get());
    }

    @Test
    void shebangに2引数版resolveで拡張子なしファイルを解決できる() {
        autoModeMap.registerShebang("ruby", () -> new StubMode("ruby"));
        var mode = autoModeMap.resolve("script", () -> "#!/usr/bin/env ruby");
        assertEquals("ruby", mode.name());
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
