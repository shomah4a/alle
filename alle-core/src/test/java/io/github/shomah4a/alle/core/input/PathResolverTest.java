package io.github.shomah4a.alle.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PathResolverTest {

    private static final Path HOME = Path.of("/home/user");

    @Nested
    class collapseTilde {

        @Test
        void HOMEディレクトリ自体がチルダに置換される() {
            assertEquals("~", PathResolver.collapseTilde("/home/user", HOME));
        }

        @Test
        void HOME配下のパスがチルダで始まる形式に置換される() {
            assertEquals("~/project/src", PathResolver.collapseTilde("/home/user/project/src", HOME));
        }

        @Test
        void HOME外のパスはそのまま返される() {
            assertEquals("/tmp/file.txt", PathResolver.collapseTilde("/tmp/file.txt", HOME));
        }

        @Test
        void HOMEと前方一致するが直下ではないパスは置換されない() {
            assertEquals("/home/user2/file.txt", PathResolver.collapseTilde("/home/user2/file.txt", HOME));
        }

        @Test
        void 末尾スラッシュ付きのHOMEパスが置換される() {
            assertEquals("~/", PathResolver.collapseTilde("/home/user/", HOME));
        }

        @Test
        void ルートパスはそのまま返される() {
            assertEquals("/", PathResolver.collapseTilde("/", HOME));
        }
    }

    @Nested
    class expandTilde {

        @Test
        void チルダのみがHOMEに展開される() {
            assertEquals("/home/user", PathResolver.expandTilde("~", HOME));
        }

        @Test
        void チルダスラッシュで始まるパスが展開される() {
            assertEquals("/home/user/project/src", PathResolver.expandTilde("~/project/src", HOME));
        }

        @Test
        void チルダで始まらないパスはそのまま返される() {
            assertEquals("/tmp/file.txt", PathResolver.expandTilde("/tmp/file.txt", HOME));
        }

        @Test
        void チルダスラッシュのみが展開される() {
            assertEquals("/home/user/", PathResolver.expandTilde("~/", HOME));
        }

        @Test
        void パス途中のチルダは展開されない() {
            assertEquals("/tmp/~/file.txt", PathResolver.expandTilde("/tmp/~/file.txt", HOME));
        }
    }

    @Nested
    class findShadowBoundary {

        @Test
        void シャドウがない場合は0を返す() {
            assertEquals(0, PathResolver.findShadowBoundary("/tmp/foo.txt"));
        }

        @Test
        void チルダで始まるパスにはシャドウがない() {
            assertEquals(0, PathResolver.findShadowBoundary("~/project/src/"));
        }

        @Test
        void スラッシュの後のチルダでシャドウが発生する() {
            assertEquals(9, PathResolver.findShadowBoundary("/foo/bar/~/baz"));
        }

        @Test
        void スラッシュの後のチルダのみでもシャドウが発生する() {
            assertEquals(9, PathResolver.findShadowBoundary("/foo/bar/~"));
        }

        @Test
        void ダブルスラッシュでシャドウが発生する() {
            assertEquals(9, PathResolver.findShadowBoundary("/foo/bar//baz"));
        }

        @Test
        void 複数のシャドウポイントがある場合は最も右のものが使われる() {
            assertEquals(6, PathResolver.findShadowBoundary("//foo//bar"));
        }

        @Test
        void ルートパスのみの場合はシャドウがない() {
            assertEquals(0, PathResolver.findShadowBoundary("/"));
        }

        @Test
        void 空文字列の場合はシャドウがない() {
            assertEquals(0, PathResolver.findShadowBoundary(""));
        }

        @Test
        void 先頭のダブルスラッシュではシャドウが発生する() {
            assertEquals(1, PathResolver.findShadowBoundary("//foo"));
        }
    }
}
