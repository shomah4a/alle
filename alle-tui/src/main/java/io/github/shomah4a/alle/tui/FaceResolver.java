package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import io.github.shomah4a.alle.core.highlight.Face;
import io.github.shomah4a.alle.core.highlight.FaceAttribute;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * Face（Named色名）をLanternaのTextColor/SGRに解決する。
 * Named色名からANSI色への固定マッピングを持つ。
 */
public class FaceResolver {

    private static final ImmutableMap<String, TextColor> COLOR_MAP = Maps.mutable
            .<String, TextColor>empty()
            .withKeyValue("default", TextColor.ANSI.DEFAULT)
            .withKeyValue("black", TextColor.ANSI.BLACK)
            .withKeyValue("red", TextColor.ANSI.RED)
            .withKeyValue("green", TextColor.ANSI.GREEN)
            .withKeyValue("yellow", TextColor.ANSI.YELLOW)
            .withKeyValue("blue", TextColor.ANSI.BLUE)
            .withKeyValue("magenta", TextColor.ANSI.MAGENTA)
            .withKeyValue("cyan", TextColor.ANSI.CYAN)
            .withKeyValue("white", TextColor.ANSI.WHITE)
            .withKeyValue("black_bright", TextColor.ANSI.BLACK_BRIGHT)
            .withKeyValue("red_bright", TextColor.ANSI.RED_BRIGHT)
            .withKeyValue("green_bright", TextColor.ANSI.GREEN_BRIGHT)
            .withKeyValue("yellow_bright", TextColor.ANSI.YELLOW_BRIGHT)
            .withKeyValue("blue_bright", TextColor.ANSI.BLUE_BRIGHT)
            .withKeyValue("magenta_bright", TextColor.ANSI.MAGENTA_BRIGHT)
            .withKeyValue("cyan_bright", TextColor.ANSI.CYAN_BRIGHT)
            .withKeyValue("white_bright", TextColor.ANSI.WHITE_BRIGHT)
            .toImmutable();

    /**
     * Named色名をLanternaのTextColorに解決する。
     * 未知の色名の場合はDEFAULTを返す。
     */
    public TextColor resolveColor(String colorName) {
        TextColor color = COLOR_MAP.get(colorName);
        return color != null ? color : TextColor.ANSI.DEFAULT;
    }

    /**
     * FaceAttributeセットをLanternaのSGRリストに変換する。
     */
    public ImmutableList<SGR> resolveSgr(ImmutableSet<FaceAttribute> attributes) {
        return attributes.collect(FaceResolver::toSgr).toList().toImmutable();
    }

    /**
     * FaceのForeground/Background/Attributesをまとめて解決する。
     */
    public ResolvedFace resolve(Face face) {
        return new ResolvedFace(
                resolveColor(face.foreground()), resolveColor(face.background()), resolveSgr(face.attributes()));
    }

    private static SGR toSgr(FaceAttribute attr) {
        return switch (attr) {
            case BOLD -> SGR.BOLD;
            case ITALIC -> SGR.ITALIC;
            case UNDERLINE -> SGR.UNDERLINE;
        };
    }

    /**
     * 解決済みのFace情報。
     */
    public record ResolvedFace(TextColor foreground, TextColor background, ImmutableList<SGR> sgrs) {}
}
