package io.github.shomah4a.alle.core.styling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.collections.api.factory.Sets;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DefaultFaceThemeTest {

    private final FaceTheme theme = new DefaultFaceTheme();

    @Nested
    class 組み込みFaceNameの解決 {

        @Test
        void DEFAULTはdefault色で属性なし() {
            var spec = theme.resolve(FaceName.DEFAULT);
            assertEquals("default", spec.foreground());
            assertEquals("default", spec.background());
            assertEquals(Sets.immutable.empty(), spec.attributes());
        }

        @Test
        void HEADINGはyellowでBOLD() {
            var spec = theme.resolve(FaceName.HEADING);
            assertEquals("yellow", spec.foreground());
            assertNull(spec.background());
            assertEquals(Sets.immutable.of(FaceAttribute.BOLD), spec.attributes());
        }

        @Test
        void STRONGは色なしでBOLD() {
            var spec = theme.resolve(FaceName.STRONG);
            assertNull(spec.foreground());
            assertNull(spec.background());
            assertEquals(Sets.immutable.of(FaceAttribute.BOLD), spec.attributes());
        }

        @Test
        void EMPHASISは色なしでITALIC() {
            var spec = theme.resolve(FaceName.EMPHASIS);
            assertNull(spec.foreground());
            assertNull(spec.background());
            assertEquals(Sets.immutable.of(FaceAttribute.ITALIC), spec.attributes());
        }

        @Test
        void DELETIONは色なしでSTRIKETHROUGH() {
            var spec = theme.resolve(FaceName.DELETION);
            assertNull(spec.foreground());
            assertNull(spec.background());
            assertEquals(Sets.immutable.of(FaceAttribute.STRIKETHROUGH), spec.attributes());
        }

        @Test
        void KEYWORDはblueでBOLD() {
            var spec = theme.resolve(FaceName.KEYWORD);
            assertEquals("blue", spec.foreground());
            assertNull(spec.background());
            assertEquals(Sets.immutable.of(FaceAttribute.BOLD), spec.attributes());
        }

        @Test
        void COMMENTはblack_brightで属性なし() {
            var spec = theme.resolve(FaceName.COMMENT);
            assertEquals("black_bright", spec.foreground());
            assertNull(spec.background());
            assertEquals(Sets.immutable.empty(), spec.attributes());
        }
    }

    @Nested
    class 未知のFaceName {

        @Test
        void 未登録のFaceNameにはDEFAULTのFaceSpecが返される() {
            var unknown = new FaceName("unknown-face", "テスト用の未知のface");
            var spec = theme.resolve(unknown);
            assertEquals("default", spec.foreground());
            assertEquals("default", spec.background());
            assertEquals(Sets.immutable.empty(), spec.attributes());
        }
    }
}
