package io.github.shomah4a.alle.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.nio.file.Path;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BufferNameUniquifierTest {

    private BufferNameUniquifier uniquifier;

    @BeforeEach
    void setUp() {
        uniquifier = new BufferNameUniquifier();
    }

    private BufferFacade createBufferWithPath(String name, Path path) {
        return new BufferFacade(new TextBuffer(name, new GapTextModel(), new SettingsRegistry(), path));
    }

    private BufferFacade createBufferWithoutPath(String name) {
        return new BufferFacade(new TextBuffer(name, new GapTextModel(), new SettingsRegistry()));
    }

    @Nested
    class 同名ファイルなし {

        @Test
        void ファイル名が異なるバッファはdisplayNameが設定されない() {
            var buf1 = createBufferWithPath("foo.txt", Path.of("/home/user/project/foo.txt"));
            var buf2 = createBufferWithPath("bar.txt", Path.of("/home/user/project/bar.txt"));
            var buffers = Lists.mutable.of(buf1, buf2);

            uniquifier.uniquify(buffers);

            assertEquals("foo.txt", buf1.getName());
            assertEquals("bar.txt", buf2.getName());
        }

        @Test
        void ファイルパスを持たないバッファは対象外() {
            var buf1 = createBufferWithoutPath("*scratch*");
            var buf2 = createBufferWithPath("foo.txt", Path.of("/home/user/project/foo.txt"));
            var buffers = Lists.mutable.of(buf1, buf2);

            uniquifier.uniquify(buffers);

            assertEquals("*scratch*", buf1.getName());
            assertEquals("foo.txt", buf2.getName());
        }
    }

    @Nested
    class 同名ファイルあり {

        @Test
        void 異なるディレクトリの同名ファイルが共通プレフィックスからの相対パスになる() {
            var buf1 = createBufferWithPath("main.py", Path.of("/home/user/project1/src/main.py"));
            var buf2 = createBufferWithPath("main.py", Path.of("/home/user/project2/src/main.py"));
            var buffers = Lists.mutable.of(buf1, buf2);

            uniquifier.uniquify(buffers);

            assertEquals("project1/src/main.py", buf1.getName());
            assertEquals("project2/src/main.py", buf2.getName());
        }

        @Test
        void 親ディレクトリのみ異なる場合はその親ディレクトリ名がつく() {
            var buf1 = createBufferWithPath("config.yml", Path.of("/home/user/app1/config.yml"));
            var buf2 = createBufferWithPath("config.yml", Path.of("/home/user/app2/config.yml"));
            var buffers = Lists.mutable.of(buf1, buf2);

            uniquifier.uniquify(buffers);

            assertEquals("app1/config.yml", buf1.getName());
            assertEquals("app2/config.yml", buf2.getName());
        }

        @Test
        void 共通プレフィックスがルートのみの場合() {
            var buf1 = createBufferWithPath("test.txt", Path.of("/aaa/test.txt"));
            var buf2 = createBufferWithPath("test.txt", Path.of("/bbb/test.txt"));
            var buffers = Lists.mutable.of(buf1, buf2);

            uniquifier.uniquify(buffers);

            assertEquals("aaa/test.txt", buf1.getName());
            assertEquals("bbb/test.txt", buf2.getName());
        }

        @Test
        void 同名ファイルが3つ以上ある場合() {
            var buf1 = createBufferWithPath("README.md", Path.of("/home/user/a/README.md"));
            var buf2 = createBufferWithPath("README.md", Path.of("/home/user/b/README.md"));
            var buf3 = createBufferWithPath("README.md", Path.of("/home/user/c/README.md"));
            var buffers = Lists.mutable.of(buf1, buf2, buf3);

            uniquifier.uniquify(buffers);

            assertEquals("a/README.md", buf1.getName());
            assertEquals("b/README.md", buf2.getName());
            assertEquals("c/README.md", buf3.getName());
        }
    }

    @Nested
    class displayNameのリセット {

        @Test
        void 同名バッファが1つになるとファイル名のみに戻る() {
            var buf1 = createBufferWithPath("main.py", Path.of("/home/user/project1/main.py"));
            var buf2 = createBufferWithPath("main.py", Path.of("/home/user/project2/main.py"));
            var buffers = Lists.mutable.of(buf1, buf2);

            uniquifier.uniquify(buffers);
            assertEquals("project1/main.py", buf1.getName());

            // buf2を除去して再uniquify
            buffers.remove(buf2);
            uniquifier.uniquify(buffers);

            assertEquals("main.py", buf1.getName());
        }
    }

    @Nested
    class computeCommonPrefix {

        @Test
        void 共通プレフィックスが正しく計算される() {
            var paths = Lists.mutable.of(
                    Path.of("/home/user/project1/src/main.py"), Path.of("/home/user/project2/src/main.py"));

            var prefix = BufferNameUniquifier.computeCommonPrefix(paths);

            assertEquals(Path.of("/home/user"), prefix);
        }

        @Test
        void 完全に一致するパスの共通プレフィックスは親ディレクトリ() {
            var paths = Lists.mutable.of(Path.of("/home/user/project/src"), Path.of("/home/user/project/src"));

            var prefix = BufferNameUniquifier.computeCommonPrefix(paths);

            assertEquals(Path.of("/home/user/project/src"), prefix);
        }

        @Test
        void ルートのみが共通の場合() {
            var paths = Lists.mutable.of(Path.of("/aaa/file.txt"), Path.of("/bbb/file.txt"));

            var prefix = BufferNameUniquifier.computeCommonPrefix(paths);

            assertEquals(Path.of("/"), prefix);
        }
    }
}
