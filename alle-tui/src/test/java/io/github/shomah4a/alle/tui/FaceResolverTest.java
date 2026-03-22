package io.github.shomah4a.alle.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import io.github.shomah4a.alle.core.styling.Face;
import io.github.shomah4a.alle.core.styling.FaceAttribute;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FaceResolverTest {

    private final FaceResolver resolver = new FaceResolver();

    @Nested
    class 色名解決 {

        @Test
        void 基本色名がANSI色に解決される() {
            assertEquals(TextColor.ANSI.RED, resolver.resolveColor("red"));
            assertEquals(TextColor.ANSI.GREEN, resolver.resolveColor("green"));
            assertEquals(TextColor.ANSI.BLUE, resolver.resolveColor("blue"));
        }

        @Test
        void bright色名がANSI_bright色に解決される() {
            assertEquals(TextColor.ANSI.RED_BRIGHT, resolver.resolveColor("red_bright"));
            assertEquals(TextColor.ANSI.WHITE_BRIGHT, resolver.resolveColor("white_bright"));
        }

        @Test
        void default色名がDEFAULTに解決される() {
            assertEquals(TextColor.ANSI.DEFAULT, resolver.resolveColor("default"));
        }

        @Test
        void 未知の色名がDEFAULTにフォールバックされる() {
            assertEquals(TextColor.ANSI.DEFAULT, resolver.resolveColor("unknown_color"));
        }
    }

    @Nested
    class SGR変換 {

        @Test
        void BOLDがSGR_BOLDに変換される() {
            var sgrs = resolver.resolveSgr(Sets.immutable.of(FaceAttribute.BOLD));
            assertEquals(Lists.immutable.of(SGR.BOLD), sgrs);
        }

        @Test
        void ITALICがSGR_ITALICに変換される() {
            var sgrs = resolver.resolveSgr(Sets.immutable.of(FaceAttribute.ITALIC));
            assertEquals(Lists.immutable.of(SGR.ITALIC), sgrs);
        }

        @Test
        void UNDERLINEがSGR_UNDERLINEに変換される() {
            var sgrs = resolver.resolveSgr(Sets.immutable.of(FaceAttribute.UNDERLINE));
            assertEquals(Lists.immutable.of(SGR.UNDERLINE), sgrs);
        }

        @Test
        void STRIKETHROUGHがSGR_CROSSED_OUTに変換される() {
            var sgrs = resolver.resolveSgr(Sets.immutable.of(FaceAttribute.STRIKETHROUGH));
            assertEquals(Lists.immutable.of(SGR.CROSSED_OUT), sgrs);
        }

        @Test
        void 空の属性セットで空リストが返される() {
            var sgrs = resolver.resolveSgr(Sets.immutable.empty());
            assertEquals(0, sgrs.size());
        }
    }

    @Nested
    class Face解決 {

        @Test
        void HEADING_Faceが前景yellow_BOLD_SGRに解決される() {
            var resolved = resolver.resolve(Face.HEADING);
            assertEquals(TextColor.ANSI.YELLOW, resolved.foreground());
            assertEquals(TextColor.ANSI.DEFAULT, resolved.background());
            assertEquals(Lists.immutable.of(SGR.BOLD), resolved.sgrs());
        }

        @Test
        void CODE_Faceが前景green_属性なしに解決される() {
            var resolved = resolver.resolve(Face.CODE);
            assertEquals(TextColor.ANSI.GREEN, resolved.foreground());
            assertEquals(TextColor.ANSI.DEFAULT, resolved.background());
            assertEquals(0, resolved.sgrs().size());
        }
    }
}
