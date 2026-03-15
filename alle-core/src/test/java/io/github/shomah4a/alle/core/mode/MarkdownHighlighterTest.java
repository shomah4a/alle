package io.github.shomah4a.alle.core.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.highlight.Face;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MarkdownHighlighterTest {

    private final MarkdownHighlighter highlighter = new MarkdownHighlighter();

    @Nested
    class 見出し {

        @Test
        void h1見出し行全体にHEADING_Faceが適用される() {
            var spans = highlighter.highlight("# Hello");

            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(7, spans.get(0).end());
            assertEquals(Face.HEADING, spans.get(0).face());
        }

        @Test
        void h3見出しも認識される() {
            var spans = highlighter.highlight("### Section");

            assertEquals(1, spans.size());
            assertEquals(Face.HEADING, spans.get(0).face());
        }

        @Test
        void シャープの後にスペースがない場合は見出しではない() {
            var spans = highlighter.highlight("#NoSpace");

            assertTrue(spans.isEmpty());
        }
    }

    @Nested
    class インラインコード {

        @Test
        void バッククォートで囲まれた部分にCODE_Faceが適用される() {
            var spans = highlighter.highlight("Use `code` here");

            assertEquals(1, spans.size());
            assertEquals(4, spans.get(0).start());
            assertEquals(10, spans.get(0).end());
            assertEquals(Face.CODE, spans.get(0).face());
        }

        @Test
        void 複数のインラインコードが認識される() {
            var spans = highlighter.highlight("`a` and `b`");

            assertEquals(2, spans.size());
            assertEquals(Face.CODE, spans.get(0).face());
            assertEquals(Face.CODE, spans.get(1).face());
        }
    }

    @Nested
    class 太字 {

        @Test
        void アスタリスク2つで囲まれた部分にBOLD_Faceが適用される() {
            var spans = highlighter.highlight("This is **bold** text");

            assertEquals(1, spans.size());
            assertEquals(8, spans.get(0).start());
            assertEquals(16, spans.get(0).end());
            assertEquals(Face.BOLD_FACE, spans.get(0).face());
        }

        @Test
        void アンダースコア2つでも太字として認識される() {
            var spans = highlighter.highlight("This is __bold__ text");

            assertEquals(1, spans.size());
            assertEquals(Face.BOLD_FACE, spans.get(0).face());
        }
    }

    @Nested
    class 斜体 {

        @Test
        void アスタリスク1つで囲まれた部分にITALIC_Faceが適用される() {
            var spans = highlighter.highlight("This is *italic* text");

            assertEquals(1, spans.size());
            assertEquals(8, spans.get(0).start());
            assertEquals(16, spans.get(0).end());
            assertEquals(Face.ITALIC_FACE, spans.get(0).face());
        }
    }

    @Nested
    class リンク {

        @Test
        void マークダウンリンクにLINK_Faceが適用される() {
            var spans = highlighter.highlight("See [link](http://example.com) here");

            assertEquals(1, spans.size());
            assertEquals(4, spans.get(0).start());
            assertEquals(30, spans.get(0).end());
            assertEquals(Face.LINK, spans.get(0).face());
        }
    }

    @Nested
    class リストマーカー {

        @Test
        void ハイフンリストマーカーにLIST_MARKER_Faceが適用される() {
            var spans = highlighter.highlight("- item");

            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(2, spans.get(0).end());
            assertEquals(Face.LIST_MARKER, spans.get(0).face());
        }

        @Test
        void 番号付きリストマーカーが認識される() {
            var spans = highlighter.highlight("1. item");

            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(3, spans.get(0).end());
            assertEquals(Face.LIST_MARKER, spans.get(0).face());
        }

        @Test
        void インデント付きリストマーカーが認識される() {
            var spans = highlighter.highlight("  - nested");

            assertEquals(1, spans.size());
            assertEquals(Face.LIST_MARKER, spans.get(0).face());
        }
    }

    @Nested
    class ルール優先順位 {

        @Test
        void 見出し行内のインラインコードは見出しFaceが優先される() {
            var spans = highlighter.highlight("# Hello `code`");

            assertEquals(1, spans.size());
            assertEquals(Face.HEADING, spans.get(0).face());
        }
    }

    @Nested
    class 通常テキスト {

        @Test
        void マークダウン記法を含まない行ではスパンが生成されない() {
            var spans = highlighter.highlight("Just plain text");

            assertTrue(spans.isEmpty());
        }
    }
}
