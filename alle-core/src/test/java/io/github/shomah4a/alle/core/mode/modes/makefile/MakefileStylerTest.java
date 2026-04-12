package io.github.shomah4a.alle.core.mode.modes.makefile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.styling.FaceName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MakefileStylerTest {

    private final MakefileStyler styler = new MakefileStyler();

    @Nested
    class コメント {

        @Test
        void シャープから行末までCOMMENT_Faceが適用される() {
            var spans = styler.styleLine("# this is a comment");
            assertEquals(1, spans.size());
            assertEquals(FaceName.COMMENT, spans.get(0).faceName());
            assertEquals(0, spans.get(0).start());
            assertEquals(19, spans.get(0).end());
        }

        @Test
        void 行中のコメントが認識される() {
            var spans = styler.styleLine("CC = gcc # compiler");
            var commentSpans = spans.select(s -> s.faceName().equals(FaceName.COMMENT));
            assertEquals(1, commentSpans.size());
        }
    }

    @Nested
    class ディレクティブ {

        @Test
        void includeがKEYWORD_Faceで認識される() {
            var spans = styler.styleLine("include common.mk");
            var keywordSpans = spans.select(s -> s.faceName().equals(FaceName.KEYWORD));
            assertEquals(1, keywordSpans.size());
        }

        @Test
        void ifeqがKEYWORD_Faceで認識される() {
            var spans = styler.styleLine("ifeq ($(OS),Windows_NT)");
            var keywordSpans = spans.select(s -> s.faceName().equals(FaceName.KEYWORD));
            assertEquals(1, keywordSpans.size());
        }

        @Test
        void endifがKEYWORD_Faceで認識される() {
            var spans = styler.styleLine("endif");
            var keywordSpans = spans.select(s -> s.faceName().equals(FaceName.KEYWORD));
            assertEquals(1, keywordSpans.size());
        }
    }

    @Nested
    class 特殊ターゲット {

        @Test
        void PHONYがBUILTIN_Faceで認識される() {
            var spans = styler.styleLine(".PHONY: all clean");
            var builtinSpans = spans.select(s -> s.faceName().equals(FaceName.BUILTIN));
            assertEquals(1, builtinSpans.size());
        }
    }

    @Nested
    class 変数定義 {

        @Test
        void イコールによる変数定義でVARIABLE_Faceが適用される() {
            var spans = styler.styleLine("CC = gcc");
            var varSpans = spans.select(s -> s.faceName().equals(FaceName.VARIABLE));
            assertTrue(varSpans.notEmpty());
        }

        @Test
        void コロンイコールによる変数定義で認識される() {
            var spans = styler.styleLine("CC := gcc");
            var varSpans = spans.select(s -> s.faceName().equals(FaceName.VARIABLE));
            assertTrue(varSpans.notEmpty());
        }

        @Test
        void クエスチョンイコールによる変数定義で認識される() {
            var spans = styler.styleLine("CC ?= gcc");
            var varSpans = spans.select(s -> s.faceName().equals(FaceName.VARIABLE));
            assertTrue(varSpans.notEmpty());
        }

        @Test
        void プラスイコールによる変数定義で認識される() {
            var spans = styler.styleLine("CFLAGS += -Wall");
            var varSpans = spans.select(s -> s.faceName().equals(FaceName.VARIABLE));
            assertTrue(varSpans.notEmpty());
        }
    }

    @Nested
    class ターゲット行 {

        @Test
        void シンプルなターゲットがFUNCTION_NAME_Faceで認識される() {
            var spans = styler.styleLine("all: main.o");
            var targetSpans = spans.select(s -> s.faceName().equals(FaceName.FUNCTION_NAME));
            assertTrue(targetSpans.notEmpty());
        }

        @Test
        void 依存関係なしのターゲットが認識される() {
            var spans = styler.styleLine("clean:");
            var targetSpans = spans.select(s -> s.faceName().equals(FaceName.FUNCTION_NAME));
            assertTrue(targetSpans.notEmpty());
        }

        @Test
        void 変数代入行はターゲットと誤判定されない() {
            var spans = styler.styleLine("CC := gcc");
            var targetSpans = spans.select(s -> s.faceName().equals(FaceName.FUNCTION_NAME));
            assertTrue(targetSpans.isEmpty());
        }

        @Test
        void クエスチョンイコールの変数代入はターゲットと誤判定されない() {
            var spans = styler.styleLine("CC ?= gcc");
            var targetSpans = spans.select(s -> s.faceName().equals(FaceName.FUNCTION_NAME));
            assertTrue(targetSpans.isEmpty());
        }

        @Test
        void プラスイコールの変数代入はターゲットと誤判定されない() {
            var spans = styler.styleLine("CFLAGS += -Wall");
            var targetSpans = spans.select(s -> s.faceName().equals(FaceName.FUNCTION_NAME));
            assertTrue(targetSpans.isEmpty());
        }
    }

    @Nested
    class 変数参照 {

        @Test
        void 丸括弧の変数参照がVARIABLE_Faceで認識される() {
            var spans = styler.styleLine("\t$(CC) -o $@ $^");
            var varSpans = spans.select(s -> s.faceName().equals(FaceName.VARIABLE));
            assertTrue(varSpans.notEmpty());
        }

        @Test
        void 波括弧の変数参照が認識される() {
            var spans = styler.styleLine("\t${CC} main.o");
            var varSpans = spans.select(s -> s.faceName().equals(FaceName.VARIABLE));
            assertTrue(varSpans.notEmpty());
        }
    }

    @Nested
    class 自動変数 {

        @Test
        void ドルアットがBUILTIN_Faceで認識される() {
            var spans = styler.styleLine("\tgcc -o $@ $<");
            var builtinSpans = spans.select(s -> s.faceName().equals(FaceName.BUILTIN));
            assertEquals(2, builtinSpans.size());
        }

        @Test
        void ドルキャレットが認識される() {
            var spans = styler.styleLine("\tgcc -o $@ $^");
            var builtinSpans = spans.select(s -> s.faceName().equals(FaceName.BUILTIN));
            assertTrue(builtinSpans.anySatisfy(s -> s.start() > 10));
        }
    }

    @Nested
    class レシピ行 {

        @Test
        void タブで始まるレシピ行内のコメントが認識される() {
            var spans = styler.styleLine("\tgcc main.c # compile");
            var commentSpans = spans.select(s -> s.faceName().equals(FaceName.COMMENT));
            assertEquals(1, commentSpans.size());
        }
    }
}
