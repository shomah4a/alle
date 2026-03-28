package io.github.shomah4a.alle.core.styling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.collections.api.factory.Sets;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FaceSpecTest {

    @Nested
    class merge {

        @Test
        void overlayのforegroundがnon_nullならbaseを上書きする() {
            var base = new FaceSpec("blue", "default", Sets.immutable.empty());
            var overlay = new FaceSpec("red", null, Sets.immutable.empty());
            var merged = base.merge(overlay);
            assertEquals("red", merged.foreground());
            assertEquals("default", merged.background());
        }

        @Test
        void overlayのforegroundがnullならbaseを維持する() {
            var base = new FaceSpec("blue", "default", Sets.immutable.empty());
            var overlay = new FaceSpec(null, null, Sets.immutable.empty());
            var merged = base.merge(overlay);
            assertEquals("blue", merged.foreground());
            assertEquals("default", merged.background());
        }

        @Test
        void overlayのbackgroundがnon_nullならbaseを上書きする() {
            var base = new FaceSpec("blue", "default", Sets.immutable.empty());
            var overlay = new FaceSpec(null, "red", Sets.immutable.empty());
            var merged = base.merge(overlay);
            assertEquals("blue", merged.foreground());
            assertEquals("red", merged.background());
        }

        @Test
        void attributesは和集合になる() {
            var base = new FaceSpec(null, null, Sets.immutable.of(FaceAttribute.BOLD));
            var overlay = new FaceSpec(null, null, Sets.immutable.of(FaceAttribute.ITALIC));
            var merged = base.merge(overlay);
            assertEquals(Sets.immutable.of(FaceAttribute.BOLD, FaceAttribute.ITALIC), merged.attributes());
        }

        @Test
        void 同じattributeが重複しても和集合なので変わらない() {
            var base = new FaceSpec(null, null, Sets.immutable.of(FaceAttribute.BOLD));
            var overlay = new FaceSpec(null, null, Sets.immutable.of(FaceAttribute.BOLD));
            var merged = base.merge(overlay);
            assertEquals(Sets.immutable.of(FaceAttribute.BOLD), merged.attributes());
        }

        @Test
        void 両方のforegroundがnullなら結果もnull() {
            var base = new FaceSpec(null, null, Sets.immutable.empty());
            var overlay = new FaceSpec(null, null, Sets.immutable.empty());
            var merged = base.merge(overlay);
            assertNull(merged.foreground());
            assertNull(merged.background());
        }

        @Test
        void HEADINGとSTRONGの合成で色が維持されattributesが合成される() {
            var heading = new FaceSpec("yellow", null, Sets.immutable.of(FaceAttribute.BOLD));
            var strong = new FaceSpec(null, null, Sets.immutable.of(FaceAttribute.BOLD));
            var merged = heading.merge(strong);
            assertEquals("yellow", merged.foreground());
            assertNull(merged.background());
            assertEquals(Sets.immutable.of(FaceAttribute.BOLD), merged.attributes());
        }
    }

    @Nested
    class ファクトリメソッド {

        @Test
        void ofForegroundは前景色のみ指定() {
            var spec = FaceSpec.ofForeground("green");
            assertEquals("green", spec.foreground());
            assertNull(spec.background());
            assertEquals(Sets.immutable.empty(), spec.attributes());
        }

        @Test
        void ofAttributesは装飾属性のみ指定() {
            var spec = FaceSpec.ofAttributes(FaceAttribute.BOLD, FaceAttribute.ITALIC);
            assertNull(spec.foreground());
            assertNull(spec.background());
            assertEquals(Sets.immutable.of(FaceAttribute.BOLD, FaceAttribute.ITALIC), spec.attributes());
        }

        @Test
        void ofは前景色と装飾属性を指定() {
            var spec = FaceSpec.of("cyan", FaceAttribute.UNDERLINE);
            assertEquals("cyan", spec.foreground());
            assertNull(spec.background());
            assertEquals(Sets.immutable.of(FaceAttribute.UNDERLINE), spec.attributes());
        }
    }
}
