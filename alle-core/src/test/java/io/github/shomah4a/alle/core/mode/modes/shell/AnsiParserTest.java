package io.github.shomah4a.alle.core.mode.modes.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AnsiParserTest {

    @Nested
    class parse {

        @Test
        void エスケープシーケンスを含まないテキストはそのまま返す() {
            var parser = new AnsiParser();
            var result = parser.parse("hello world");
            assertEquals(1, result.size());
            assertEquals("hello world", result.get(0).text());
            assertNull(result.get(0).face());
        }

        @Test
        void SGRで前景色を設定したテキストをパースする() {
            var parser = new AnsiParser();
            var result = parser.parse("\u001b[31mred text\u001b[0m");
            assertEquals(1, result.size());
            assertEquals("red text", result.get(0).text());
            var face = result.get(0).face();
            assertNotNull(face);
            assertEquals("ansi-sgr:fg=red", face.name());
        }

        @Test
        void リセット後のテキストはデフォルトスタイルになる() {
            var parser = new AnsiParser();
            var result = parser.parse("\u001b[31mred\u001b[0m normal");
            assertEquals(2, result.size());
            assertEquals("red", result.get(0).text());
            var face0 = result.get(0).face();
            assertNotNull(face0);
            assertEquals("ansi-sgr:fg=red", face0.name());
            assertEquals(" normal", result.get(1).text());
            assertNull(result.get(1).face());
        }

        @Test
        void 複数のSGRコードをセミコロン区切りで処理する() {
            var parser = new AnsiParser();
            var result = parser.parse("\u001b[1;32mbold green\u001b[0m");
            assertEquals(1, result.size());
            assertEquals("bold green", result.get(0).text());
            var face = result.get(0).face();
            assertNotNull(face);
            assertEquals("ansi-sgr:fg=green:bold", face.name());
        }

        @Test
        void パラメータなしのSGRはリセットとして扱う() {
            var parser = new AnsiParser();
            var result = parser.parse("\u001b[31mred\u001b[mnormal");
            assertEquals(2, result.size());
            assertEquals("red", result.get(0).text());
            var face0 = result.get(0).face();
            assertNotNull(face0);
            assertEquals("ansi-sgr:fg=red", face0.name());
            assertEquals("normal", result.get(1).text());
            assertNull(result.get(1).face());
        }

        @Test
        void SGR以外のCSIシーケンスは除去する() {
            var parser = new AnsiParser();
            var result = parser.parse("\u001b[Hhello\u001b[2Jworld");
            assertEquals(2, result.size());
            assertEquals("hello", result.get(0).text());
            assertNull(result.get(0).face());
            assertEquals("world", result.get(1).text());
            assertNull(result.get(1).face());
        }

        @Test
        void OSCシーケンスをBEL終端で除去する() {
            var parser = new AnsiParser();
            // ESC ] 0;title BEL
            var result = parser.parse("\u001b]0;terminal title\u0007prompt$ ");
            assertEquals(1, result.size());
            assertEquals("prompt$ ", result.get(0).text());
        }

        @Test
        void OSCシーケンスをESCバックスラッシュ終端で除去する() {
            var parser = new AnsiParser();
            // ESC ] 0;title ESC \
            var result = parser.parse("\u001b]0;terminal title\u001b\\prompt$ ");
            assertEquals(1, result.size());
            assertEquals("prompt$ ", result.get(0).text());
        }

        @Test
        void 行をまたぐSGR状態を保持する() {
            var parser = new AnsiParser();
            parser.parse("\u001b[31m");
            var result = parser.parse("still red");
            assertEquals(1, result.size());
            assertEquals("still red", result.get(0).text());
            var face = result.get(0).face();
            assertNotNull(face);
            assertEquals("ansi-sgr:fg=red", face.name());
        }

        @Test
        void 行をまたいだ後にリセットする() {
            var parser = new AnsiParser();
            parser.parse("\u001b[31m");
            var result = parser.parse("red\u001b[0m normal");
            assertEquals(2, result.size());
            assertEquals("red", result.get(0).text());
            var face0 = result.get(0).face();
            assertNotNull(face0);
            assertEquals("ansi-sgr:fg=red", face0.name());
            assertEquals(" normal", result.get(1).text());
            assertNull(result.get(1).face());
        }

        @Test
        void 空のテキストセグメントは含まない() {
            var parser = new AnsiParser();
            var result = parser.parse("\u001b[31m\u001b[0m");
            assertEquals(0, result.size());
        }

        @Test
        void 前景色と背景色を同時に設定する() {
            var parser = new AnsiParser();
            var result = parser.parse("\u001b[31;42mtext\u001b[0m");
            assertEquals(1, result.size());
            assertEquals("text", result.get(0).text());
            var face = result.get(0).face();
            assertNotNull(face);
            assertEquals("ansi-sgr:fg=red:bg=green", face.name());
        }

        @Test
        void 明るい色コードをパースする() {
            var parser = new AnsiParser();
            var result = parser.parse("\u001b[91mbright red\u001b[0m");
            assertEquals(1, result.size());
            assertEquals("bright red", result.get(0).text());
            var face = result.get(0).face();
            assertNotNull(face);
            assertEquals("ansi-sgr:fg=red_bright", face.name());
        }

        @Test
        void エスケープシーケンスの前のテキストを保持する() {
            var parser = new AnsiParser();
            var result = parser.parse("before \u001b[31mred\u001b[0m after");
            assertEquals(3, result.size());
            assertEquals("before ", result.get(0).text());
            assertNull(result.get(0).face());
            assertEquals("red", result.get(1).text());
            var face1 = result.get(1).face();
            assertNotNull(face1);
            assertEquals("ansi-sgr:fg=red", face1.name());
            assertEquals(" after", result.get(2).text());
            assertNull(result.get(2).face());
        }
    }
}
