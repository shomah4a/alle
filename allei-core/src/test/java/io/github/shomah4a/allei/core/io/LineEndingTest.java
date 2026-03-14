package io.github.shomah4a.allei.core.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LineEndingTest {

    @Nested
    class detect {

        @Test
        void LFを検出できる() {
            assertEquals(LineEnding.LF, LineEnding.detect("Hello\nWorld"));
        }

        @Test
        void CRLFを検出できる() {
            assertEquals(LineEnding.CRLF, LineEnding.detect("Hello\r\nWorld"));
        }

        @Test
        void CRを検出できる() {
            assertEquals(LineEnding.CR, LineEnding.detect("Hello\rWorld"));
        }

        @Test
        void 改行がない場合はLFを返す() {
            assertEquals(LineEnding.LF, LineEnding.detect("Hello"));
        }

        @Test
        void 空文字列ではLFを返す() {
            assertEquals(LineEnding.LF, LineEnding.detect(""));
        }
    }

    @Nested
    class normalize {

        @Test
        void CRLFをLFに正規化する() {
            assertEquals("Hello\nWorld\n", LineEnding.normalize("Hello\r\nWorld\r\n"));
        }

        @Test
        void CRをLFに正規化する() {
            assertEquals("Hello\nWorld\n", LineEnding.normalize("Hello\rWorld\r"));
        }

        @Test
        void LFはそのまま() {
            assertEquals("Hello\nWorld", LineEnding.normalize("Hello\nWorld"));
        }

        @Test
        void 改行がない文字列はそのまま() {
            assertEquals("Hello", LineEnding.normalize("Hello"));
        }
    }

    @Nested
    class denormalize {

        @Test
        void LFからCRLFに変換する() {
            assertEquals("Hello\r\nWorld", LineEnding.CRLF.denormalize("Hello\nWorld"));
        }

        @Test
        void LFからCRに変換する() {
            assertEquals("Hello\rWorld", LineEnding.CR.denormalize("Hello\nWorld"));
        }

        @Test
        void LFはそのまま() {
            assertEquals("Hello\nWorld", LineEnding.LF.denormalize("Hello\nWorld"));
        }
    }
}
