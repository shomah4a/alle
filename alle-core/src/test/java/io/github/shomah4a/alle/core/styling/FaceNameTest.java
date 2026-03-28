package io.github.shomah4a.alle.core.styling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FaceNameTest {

    @Nested
    class equalsとhashCode {

        @Test
        void 同じnameを持つインスタンスは等しい() {
            var a = new FaceName("keyword", "説明A");
            var b = new FaceName("keyword", "説明B");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void 異なるnameを持つインスタンスは等しくない() {
            var a = new FaceName("keyword", "説明");
            var b = new FaceName("comment", "説明");
            assertNotEquals(a, b);
        }

        @Test
        void 定数と同じnameで生成したインスタンスは定数と等しい() {
            var custom = new FaceName("keyword", "カスタム説明");
            assertEquals(FaceName.KEYWORD, custom);
        }
    }

    @Nested
    class アクセサ {

        @Test
        void nameとdescriptionが取得できる() {
            assertEquals("keyword", FaceName.KEYWORD.name());
            assertEquals("プログラミング言語のキーワード", FaceName.KEYWORD.description());
        }
    }

    @Nested
    class toString表現 {

        @Test
        void nameを含む文字列が返される() {
            assertEquals("FaceName[keyword]", FaceName.KEYWORD.toString());
        }
    }
}
