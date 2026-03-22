package io.github.shomah4a.alle.core.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.styling.Face;
import io.github.shomah4a.alle.core.styling.StylingState;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MarkdownStylerTest {

    private final MarkdownStyler styler = new MarkdownStyler();

    @Nested
    class 見出し {

        @Test
        void h1見出し行全体にHEADING_Faceが適用される() {
            var spans = styler.styleLine("# Hello");

            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(7, spans.get(0).end());
            assertEquals(Face.HEADING, spans.get(0).face());
        }

        @Test
        void h3見出しも認識される() {
            var spans = styler.styleLine("### Section");

            assertEquals(1, spans.size());
            assertEquals(Face.HEADING, spans.get(0).face());
        }

        @Test
        void シャープの後にスペースがない場合は見出しではない() {
            var spans = styler.styleLine("#NoSpace");

            assertTrue(spans.isEmpty());
        }
    }

    @Nested
    class インラインコード {

        @Test
        void バッククォートで囲まれた部分にCODE_Faceが適用される() {
            var spans = styler.styleLine("Use `code` here");

            assertEquals(1, spans.size());
            assertEquals(4, spans.get(0).start());
            assertEquals(10, spans.get(0).end());
            assertEquals(Face.CODE, spans.get(0).face());
        }

        @Test
        void 複数のインラインコードが認識される() {
            var spans = styler.styleLine("`a` and `b`");

            assertEquals(2, spans.size());
            assertEquals(Face.CODE, spans.get(0).face());
            assertEquals(Face.CODE, spans.get(1).face());
        }
    }

    @Nested
    class 太字 {

        @Test
        void アスタリスク2つで囲まれた部分にBOLD_Faceが適用される() {
            var spans = styler.styleLine("This is **bold** text");

            assertEquals(1, spans.size());
            assertEquals(8, spans.get(0).start());
            assertEquals(16, spans.get(0).end());
            assertEquals(Face.BOLD_FACE, spans.get(0).face());
        }

        @Test
        void アンダースコア2つでも太字として認識される() {
            var spans = styler.styleLine("This is __bold__ text");

            assertEquals(1, spans.size());
            assertEquals(Face.BOLD_FACE, spans.get(0).face());
        }
    }

    @Nested
    class 斜体 {

        @Test
        void アスタリスク1つで囲まれた部分にITALIC_Faceが適用される() {
            var spans = styler.styleLine("This is *italic* text");

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
            var spans = styler.styleLine("See [link](http://example.com) here");

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
            var spans = styler.styleLine("- item");

            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(2, spans.get(0).end());
            assertEquals(Face.LIST_MARKER, spans.get(0).face());
        }

        @Test
        void 番号付きリストマーカーが認識される() {
            var spans = styler.styleLine("1. item");

            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(3, spans.get(0).end());
            assertEquals(Face.LIST_MARKER, spans.get(0).face());
        }

        @Test
        void インデント付きリストマーカーが認識される() {
            var spans = styler.styleLine("  - nested");

            assertEquals(1, spans.size());
            assertEquals(Face.LIST_MARKER, spans.get(0).face());
        }
    }

    @Nested
    class ルール優先順位 {

        @Test
        void 見出し行内のインラインコードは見出しFaceが優先される() {
            var spans = styler.styleLine("# Hello `code`");

            assertEquals(1, spans.size());
            assertEquals(Face.HEADING, spans.get(0).face());
        }
    }

    @Nested
    class コードブロック {

        @Test
        void バッククォート3つの行でリージョンが開始される() {
            var result = styler.styleLineWithState("```java", StylingState.NONE);

            assertEquals(1, result.spans().size());
            assertEquals(Face.CODE, result.spans().get(0).face());
            assertTrue(result.nextState().isInRegion());
        }

        @Test
        void コードブロック内の行全体にCODE_Faceが適用される() {
            var r1 = styler.styleLineWithState("```", StylingState.NONE);
            var r2 = styler.styleLineWithState("int x = 1;", r1.nextState());

            assertEquals(1, r2.spans().size());
            assertEquals(0, r2.spans().get(0).start());
            assertEquals(10, r2.spans().get(0).end());
            assertEquals(Face.CODE, r2.spans().get(0).face());
            assertTrue(r2.nextState().isInRegion());
        }

        @Test
        void コードブロック終了行でリージョンが終了する() {
            var r1 = styler.styleLineWithState("```", StylingState.NONE);
            var r2 = styler.styleLineWithState("code", r1.nextState());
            var r3 = styler.styleLineWithState("```", r2.nextState());

            assertFalse(r3.nextState().isInRegion());
        }

        @Test
        void コードブロック内では他のマークダウン記法が無視される() {
            var r1 = styler.styleLineWithState("```", StylingState.NONE);
            var r2 = styler.styleLineWithState("# heading", r1.nextState());

            assertEquals(1, r2.spans().size());
            assertEquals(Face.CODE, r2.spans().get(0).face());
        }

        @Test
        void コードブロック終了後に通常のマークダウン記法が有効になる() {
            var r1 = styler.styleLineWithState("```", StylingState.NONE);
            var r2 = styler.styleLineWithState("code", r1.nextState());
            var r3 = styler.styleLineWithState("```", r2.nextState());
            var r4 = styler.styleLineWithState("# heading", r3.nextState());

            assertEquals(1, r4.spans().size());
            assertEquals(Face.HEADING, r4.spans().get(0).face());
            assertFalse(r4.nextState().isInRegion());
        }

        @Test
        void コードブロック内の空行でリージョンが維持される() {
            var r1 = styler.styleLineWithState("```", StylingState.NONE);
            var r2 = styler.styleLineWithState("", r1.nextState());

            assertTrue(r2.spans().isEmpty());
            assertTrue(r2.nextState().isInRegion());
        }

        @Test
        void 言語指定付きコードブロック開始行全体にCODE_Faceが適用される() {
            var result = styler.styleLineWithState("```python", StylingState.NONE);

            assertEquals(1, result.spans().size());
            assertEquals(0, result.spans().get(0).start());
            assertEquals(9, result.spans().get(0).end());
            assertEquals(Face.CODE, result.spans().get(0).face());
        }

        @Test
        void 行中のバッククォート3つはコードブロック開始にならない() {
            var result = styler.styleLineWithState("text ``` more", StylingState.NONE);

            assertFalse(result.nextState().isInRegion());
        }

        @Test
        void コードブロックの開始と終了を繰り返した場合に状態が正しく遷移する() {
            var r1 = styler.styleLineWithState("```", StylingState.NONE);
            assertTrue(r1.nextState().isInRegion());

            var r2 = styler.styleLineWithState("block1", r1.nextState());
            assertTrue(r2.nextState().isInRegion());

            var r3 = styler.styleLineWithState("```", r2.nextState());
            assertFalse(r3.nextState().isInRegion());

            var r4 = styler.styleLineWithState("normal text", r3.nextState());
            assertFalse(r4.nextState().isInRegion());

            var r5 = styler.styleLineWithState("```", r4.nextState());
            assertTrue(r5.nextState().isInRegion());

            var r6 = styler.styleLineWithState("block2", r5.nextState());
            assertTrue(r6.nextState().isInRegion());

            var r7 = styler.styleLineWithState("```", r6.nextState());
            assertFalse(r7.nextState().isInRegion());
        }

        @Test
        void コードブロック内のインラインコードが無視される() {
            var r1 = styler.styleLineWithState("```", StylingState.NONE);
            var r2 = styler.styleLineWithState("use `code` here", r1.nextState());

            assertEquals(1, r2.spans().size());
            assertEquals(0, r2.spans().get(0).start());
            assertEquals(15, r2.spans().get(0).end());
            assertEquals(Face.CODE, r2.spans().get(0).face());
        }

        @Test
        void コードブロック内のリンクが無視される() {
            var r1 = styler.styleLineWithState("```", StylingState.NONE);
            var r2 = styler.styleLineWithState("[link](url)", r1.nextState());

            assertEquals(1, r2.spans().size());
            assertEquals(Face.CODE, r2.spans().get(0).face());
        }
    }

    @Nested
    class 水平線 {

        @Test
        void ハイフン3つで水平線が認識される() {
            var spans = styler.styleLine("---");

            assertEquals(1, spans.size());
            assertEquals(Face.COMMENT, spans.get(0).face());
        }

        @Test
        void アスタリスク3つで水平線が認識される() {
            var spans = styler.styleLine("***");

            assertEquals(1, spans.size());
            assertEquals(Face.COMMENT, spans.get(0).face());
        }

        @Test
        void アンダースコア3つで水平線が認識される() {
            var spans = styler.styleLine("___");

            assertEquals(1, spans.size());
            assertEquals(Face.COMMENT, spans.get(0).face());
        }

        @Test
        void スペース付きの水平線が認識される() {
            var spans = styler.styleLine("- - -");

            assertEquals(1, spans.size());
            assertEquals(Face.COMMENT, spans.get(0).face());
        }
    }

    @Nested
    class 引用 {

        @Test
        void 引用行全体にSTRING_Faceが適用される() {
            var spans = styler.styleLine("> quoted text");

            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(13, spans.get(0).end());
            assertEquals(Face.STRING, spans.get(0).face());
        }

        @Test
        void 空引用が認識される() {
            var spans = styler.styleLine(">");

            assertEquals(1, spans.size());
            assertEquals(Face.STRING, spans.get(0).face());
        }
    }

    @Nested
    class 画像リンク {

        @Test
        void 画像リンクにLINK_Faceが適用される() {
            var spans = styler.styleLine("See ![alt](image.png) here");

            assertEquals(1, spans.size());
            assertEquals(4, spans.get(0).start());
            assertEquals(21, spans.get(0).end());
            assertEquals(Face.LINK, spans.get(0).face());
        }
    }

    @Nested
    class テーブル {

        @Test
        void テーブル区切り行全体にTABLE_Faceが適用される() {
            // "|---|---|" は9文字
            var spans = styler.styleLine("|---|---|");

            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(9, spans.get(0).end());
            assertEquals(Face.TABLE, spans.get(0).face());
        }

        @Test
        void アライメント指定付き区切り行が認識される() {
            var spans = styler.styleLine("| :--- | :---: | ---: |");

            assertEquals(1, spans.size());
            assertEquals(Face.TABLE, spans.get(0).face());
        }

        @Test
        void パイプなし区切り行が認識される() {
            var spans = styler.styleLine("--- | --- | ---");

            assertEquals(1, spans.size());
            assertEquals(Face.TABLE, spans.get(0).face());
        }

        @Test
        void テーブルデータ行のパイプ記号にTABLE_Faceが適用される() {
            var spans = styler.styleLine("| cell1 | cell2 |");

            // パイプ3つ
            assertEquals(3, spans.size());
            for (var span : spans) {
                assertEquals(Face.TABLE, span.face());
                assertEquals(1, span.end() - span.start());
            }
        }

        @Test
        void テーブルヘッダ行のパイプ記号にTABLE_Faceが適用される() {
            var spans = styler.styleLine("| Header 1 | Header 2 |");

            assertEquals(3, spans.size());
            for (var span : spans) {
                assertEquals(Face.TABLE, span.face());
            }
        }
    }

    @Nested
    class 通常テキスト {

        @Test
        void マークダウン記法を含まない行ではスパンが生成されない() {
            var spans = styler.styleLine("Just plain text");

            assertTrue(spans.isEmpty());
        }
    }
}
