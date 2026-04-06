package io.github.shomah4a.alle.core.mode.modes.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.styling.FaceName;
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
            assertEquals(FaceName.HEADING, spans.get(0).faceName());
        }

        @Test
        void h3見出しも認識される() {
            var spans = styler.styleLine("### Section");

            assertEquals(1, spans.size());
            assertEquals(FaceName.HEADING, spans.get(0).faceName());
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
            assertEquals(FaceName.CODE, spans.get(0).faceName());
        }

        @Test
        void 複数のインラインコードが認識される() {
            var spans = styler.styleLine("`a` and `b`");

            assertEquals(2, spans.size());
            assertEquals(FaceName.CODE, spans.get(0).faceName());
            assertEquals(FaceName.CODE, spans.get(1).faceName());
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
            assertEquals(FaceName.STRONG, spans.get(0).faceName());
        }

        @Test
        void アンダースコア2つでも太字として認識される() {
            var spans = styler.styleLine("This is __bold__ text");

            assertEquals(1, spans.size());
            assertEquals(FaceName.STRONG, spans.get(0).faceName());
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
            assertEquals(FaceName.EMPHASIS, spans.get(0).faceName());
        }
    }

    @Nested
    class 取り消し線 {

        @Test
        void チルダ2つで囲まれた部分にSTRIKETHROUGH_Faceが適用される() {
            var spans = styler.styleLine("This is ~~deleted~~ text");

            assertEquals(1, spans.size());
            assertEquals(8, spans.get(0).start());
            assertEquals(19, spans.get(0).end());
            assertEquals(FaceName.DELETION, spans.get(0).faceName());
        }

        @Test
        void 複数の取り消し線が認識される() {
            var spans = styler.styleLine("~~a~~ and ~~b~~");

            assertEquals(2, spans.size());
            assertEquals(FaceName.DELETION, spans.get(0).faceName());
            assertEquals(FaceName.DELETION, spans.get(1).faceName());
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
            assertEquals(FaceName.LINK, spans.get(0).faceName());
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
            assertEquals(FaceName.LIST_MARKER, spans.get(0).faceName());
        }

        @Test
        void 番号付きリストマーカーが認識される() {
            var spans = styler.styleLine("1. item");

            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(3, spans.get(0).end());
            assertEquals(FaceName.LIST_MARKER, spans.get(0).faceName());
        }

        @Test
        void インデント付きリストマーカーが認識される() {
            var spans = styler.styleLine("  - nested");

            assertEquals(1, spans.size());
            assertEquals(FaceName.LIST_MARKER, spans.get(0).faceName());
        }
    }

    @Nested
    class ルール優先順位 {

        @Test
        void 見出し行内のインラインコードは見出しFaceが優先される() {
            var spans = styler.styleLine("# Hello `code`");

            assertEquals(1, spans.size());
            assertEquals(FaceName.HEADING, spans.get(0).faceName());
        }
    }

    @Nested
    class コードブロック {

        @Test
        void バッククォート3つの行でリージョンが開始される() {
            var result = styler.styleLineWithState("```java", StylingState.NONE);

            assertEquals(1, result.spans().size());
            assertEquals(FaceName.CODE, result.spans().get(0).faceName());
            assertTrue(result.nextState().isInRegion());
        }

        @Test
        void コードブロック内の行全体にCODE_Faceが適用される() {
            var r1 = styler.styleLineWithState("```", StylingState.NONE);
            var r2 = styler.styleLineWithState("int x = 1;", r1.nextState());

            assertEquals(1, r2.spans().size());
            assertEquals(0, r2.spans().get(0).start());
            assertEquals(10, r2.spans().get(0).end());
            assertEquals(FaceName.CODE, r2.spans().get(0).faceName());
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
            assertEquals(FaceName.CODE, r2.spans().get(0).faceName());
        }

        @Test
        void コードブロック終了後に通常のマークダウン記法が有効になる() {
            var r1 = styler.styleLineWithState("```", StylingState.NONE);
            var r2 = styler.styleLineWithState("code", r1.nextState());
            var r3 = styler.styleLineWithState("```", r2.nextState());
            var r4 = styler.styleLineWithState("# heading", r3.nextState());

            assertEquals(1, r4.spans().size());
            assertEquals(FaceName.HEADING, r4.spans().get(0).faceName());
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
            assertEquals(FaceName.CODE, result.spans().get(0).faceName());
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
            assertEquals(FaceName.CODE, r2.spans().get(0).faceName());
        }

        @Test
        void コードブロック内のリンクが無視される() {
            var r1 = styler.styleLineWithState("```", StylingState.NONE);
            var r2 = styler.styleLineWithState("[link](url)", r1.nextState());

            assertEquals(1, r2.spans().size());
            assertEquals(FaceName.CODE, r2.spans().get(0).faceName());
        }
    }

    @Nested
    class 水平線 {

        @Test
        void ハイフン3つで水平線が認識される() {
            var spans = styler.styleLine("---");

            assertEquals(1, spans.size());
            assertEquals(FaceName.COMMENT, spans.get(0).faceName());
        }

        @Test
        void アスタリスク3つで水平線が認識される() {
            var spans = styler.styleLine("***");

            assertEquals(1, spans.size());
            assertEquals(FaceName.COMMENT, spans.get(0).faceName());
        }

        @Test
        void アンダースコア3つで水平線が認識される() {
            var spans = styler.styleLine("___");

            assertEquals(1, spans.size());
            assertEquals(FaceName.COMMENT, spans.get(0).faceName());
        }

        @Test
        void スペース付きの水平線が認識される() {
            var spans = styler.styleLine("- - -");

            assertEquals(1, spans.size());
            assertEquals(FaceName.COMMENT, spans.get(0).faceName());
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
            assertEquals(FaceName.STRING, spans.get(0).faceName());
        }

        @Test
        void 空引用が認識される() {
            var spans = styler.styleLine(">");

            assertEquals(1, spans.size());
            assertEquals(FaceName.STRING, spans.get(0).faceName());
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
            assertEquals(FaceName.LINK, spans.get(0).faceName());
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
            assertEquals(FaceName.TABLE, spans.get(0).faceName());
        }

        @Test
        void アライメント指定付き区切り行が認識される() {
            var spans = styler.styleLine("| :--- | :---: | ---: |");

            assertEquals(1, spans.size());
            assertEquals(FaceName.TABLE, spans.get(0).faceName());
        }

        @Test
        void パイプなし区切り行が認識される() {
            var spans = styler.styleLine("--- | --- | ---");

            assertEquals(1, spans.size());
            assertEquals(FaceName.TABLE, spans.get(0).faceName());
        }

        @Test
        void テーブルデータ行のパイプ記号にTABLE_Faceが適用される() {
            var spans = styler.styleLine("| cell1 | cell2 |");

            // パイプ3つ
            assertEquals(3, spans.size());
            for (var span : spans) {
                assertEquals(FaceName.TABLE, span.faceName());
                assertEquals(1, span.end() - span.start());
            }
        }

        @Test
        void テーブルヘッダ行のパイプ記号にTABLE_Faceが適用される() {
            var spans = styler.styleLine("| Header 1 | Header 2 |");

            assertEquals(3, spans.size());
            for (var span : spans) {
                assertEquals(FaceName.TABLE, span.faceName());
            }
        }
    }

    @Nested
    class タスクリスト {

        @Test
        void 未完了タスクリストのチェックボックスにLIST_MARKER_Faceが適用される() {
            var spans = styler.styleLine("- [ ] todo item");

            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            // "- [ ] " = 6文字
            assertEquals(6, spans.get(0).end());
            assertEquals(FaceName.LIST_MARKER, spans.get(0).faceName());
        }

        @Test
        void 完了タスクリストのチェックボックスにLIST_MARKER_Faceが適用される() {
            var spans = styler.styleLine("- [x] done item");

            assertEquals(1, spans.size());
            assertEquals(FaceName.LIST_MARKER, spans.get(0).faceName());
        }

        @Test
        void 大文字Xのタスクリストが認識される() {
            var spans = styler.styleLine("- [X] done item");

            assertEquals(1, spans.size());
            assertEquals(FaceName.LIST_MARKER, spans.get(0).faceName());
        }

        @Test
        void アスタリスクのタスクリストが認識される() {
            var spans = styler.styleLine("* [ ] todo");

            assertEquals(1, spans.size());
            assertEquals(FaceName.LIST_MARKER, spans.get(0).faceName());
        }
    }

    @Nested
    class 参照リンク {

        @Test
        void 参照リンク定義行全体にLINK_Faceが適用される() {
            var spans = styler.styleLine("[ref]: https://example.com");

            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(26, spans.get(0).end());
            assertEquals(FaceName.LINK, spans.get(0).faceName());
        }

        @Test
        void インライン参照リンクにLINK_Faceが適用される() {
            var spans = styler.styleLine("See [link text][ref] here");

            assertEquals(1, spans.size());
            assertEquals(4, spans.get(0).start());
            assertEquals(20, spans.get(0).end());
            assertEquals(FaceName.LINK, spans.get(0).faceName());
        }

        @Test
        void 空参照リンクが認識される() {
            var spans = styler.styleLine("See [link text][] here");

            assertEquals(1, spans.size());
            assertEquals(FaceName.LINK, spans.get(0).faceName());
        }
    }

    @Nested
    class HTMLコメント {

        @Test
        void 同一行のHTMLコメントにCOMMENT_Faceが適用される() {
            // "text <!-- comment --> more" で <!-- は位置5、--> の末尾は位置21
            var spans = styler.styleLine("text <!-- comment --> more");

            assertEquals(1, spans.size());
            assertEquals(5, spans.get(0).start());
            assertEquals(21, spans.get(0).end());
            assertEquals(FaceName.COMMENT, spans.get(0).faceName());
        }

        @Test
        void 複数行HTMLコメントの開始行でリージョンが開始される() {
            var result = styler.styleLineWithState("<!-- start", StylingState.NONE);

            assertEquals(1, result.spans().size());
            assertEquals(FaceName.COMMENT, result.spans().get(0).faceName());
            assertTrue(result.nextState().isInRegion());
        }

        @Test
        void 複数行HTMLコメント内の行全体にCOMMENT_Faceが適用される() {
            var r1 = styler.styleLineWithState("<!-- start", StylingState.NONE);
            var r2 = styler.styleLineWithState("middle", r1.nextState());

            assertEquals(1, r2.spans().size());
            assertEquals(FaceName.COMMENT, r2.spans().get(0).faceName());
            assertTrue(r2.nextState().isInRegion());
        }

        @Test
        void 複数行HTMLコメントの終了行でリージョンが終了する() {
            var r1 = styler.styleLineWithState("<!-- start", StylingState.NONE);
            var r2 = styler.styleLineWithState("end -->", r1.nextState());

            assertFalse(r2.nextState().isInRegion());
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
