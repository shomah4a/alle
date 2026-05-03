package io.github.shomah4a.alle.core.styling;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.ImmutableMap;

/**
 * 組み込みのデフォルトテーマ。
 * ここにはモード非依存の汎用セマンティック名のみを定義する。
 * 各モードのスタイラーはここで定義されたセマンティック名から選択して使用すること。
 * モード固有のFaceNameを追加してはならない。
 */
public class DefaultFaceTheme implements FaceTheme {

    private static final FaceSpec DEFAULT_SPEC = new FaceSpec("default", "default", Sets.immutable.empty());

    private static final ImmutableMap<FaceName, FaceSpec> MAPPING = Maps.mutable
            .<FaceName, FaceSpec>empty()
            .withKeyValue(FaceName.DEFAULT, DEFAULT_SPEC)
            .withKeyValue(FaceName.HEADING, FaceSpec.of("yellow", FaceAttribute.BOLD))
            .withKeyValue(FaceName.CODE, FaceSpec.ofForeground("green"))
            .withKeyValue(FaceName.LINK, FaceSpec.of("cyan", FaceAttribute.UNDERLINE))
            .withKeyValue(FaceName.LIST_MARKER, FaceSpec.ofForeground("magenta"))
            .withKeyValue(FaceName.COMMENT, FaceSpec.ofForeground("black_bright"))
            .withKeyValue(FaceName.KEYWORD, FaceSpec.of("blue", FaceAttribute.BOLD))
            .withKeyValue(FaceName.STRING, FaceSpec.ofForeground("green"))
            .withKeyValue(FaceName.TABLE, FaceSpec.ofForeground("cyan"))
            .withKeyValue(FaceName.NUMBER, FaceSpec.ofForeground("cyan"))
            .withKeyValue(FaceName.ANNOTATION, FaceSpec.ofForeground("magenta"))
            .withKeyValue(FaceName.TYPE, FaceSpec.ofForeground("yellow"))
            .withKeyValue(FaceName.FUNCTION_NAME, FaceSpec.ofForeground("cyan"))
            .withKeyValue(FaceName.VARIABLE, FaceSpec.ofForeground("yellow"))
            .withKeyValue(FaceName.OPERATOR, FaceSpec.ofForeground("white"))
            .withKeyValue(FaceName.BUILTIN, FaceSpec.ofForeground("cyan"))
            .withKeyValue(FaceName.STRONG, FaceSpec.ofAttributes(FaceAttribute.BOLD))
            .withKeyValue(FaceName.EMPHASIS, FaceSpec.ofAttributes(FaceAttribute.ITALIC))
            .withKeyValue(FaceName.DELETION, FaceSpec.ofAttributes(FaceAttribute.STRIKETHROUGH))
            .withKeyValue(FaceName.PROMPT, FaceSpec.of("cyan", FaceAttribute.BOLD))
            .withKeyValue(FaceName.DIRECTORY, FaceSpec.of("blue", FaceAttribute.BOLD))
            .withKeyValue(FaceName.FILE, DEFAULT_SPEC)
            .withKeyValue(FaceName.MARKED, FaceSpec.of("yellow", FaceAttribute.BOLD))
            .withKeyValue(FaceName.LINE_NUMBER, FaceSpec.ofForeground("black_bright"))
            .withKeyValue(FaceName.WARNING, FaceSpec.ofForeground("red"))
            .withKeyValue(
                    FaceName.ISEARCH_MATCH, new FaceSpec("black", "yellow", Sets.immutable.with(FaceAttribute.BOLD)))
            .withKeyValue(
                    FaceName.QUERY_REPLACE_MATCH,
                    new FaceSpec("black", "yellow", Sets.immutable.with(FaceAttribute.BOLD)))
            .withKeyValue(FaceName.FILE_NAME_SHADOW, FaceSpec.ofForeground("black_bright"))
            .withKeyValue(FaceName.DIFF_ADDED, FaceSpec.ofForeground("green"))
            .withKeyValue(FaceName.DIFF_MODIFIED, FaceSpec.ofForeground("yellow"))
            .withKeyValue(FaceName.DIFF_DELETED, FaceSpec.ofForeground("red"))
            .toImmutable();

    private static final String ANSI_SGR_PREFIX = "ansi-sgr:";

    @Override
    public FaceSpec resolve(FaceName name) {
        FaceSpec spec = MAPPING.get(name);
        if (spec != null) {
            return spec;
        }
        if (name.name().startsWith(ANSI_SGR_PREFIX)) {
            return parseSgrFaceSpec(name.name());
        }
        return DEFAULT_SPEC;
    }

    /**
     * {@code "ansi-sgr:"} プレフィックス付きの FaceName 名からFaceSpecを���築する。
     *
     * <p>規約: {@code ansi-sgr:} の後���コロン区切りで以下の属性を連結する。
     * <ul>
     *   <li>{@code fg=<色名>} - 前景色</li>
     *   <li>{@code bg=<色名>} - 背景色</li>
     *   <li>{@code bold} - 太字</li>
     *   <li>{@code underline} - 下線</li>
     * </ul>
     */
    private static FaceSpec parseSgrFaceSpec(String faceName) {
        String body = faceName.substring(ANSI_SGR_PREFIX.length());
        String fg = null;
        String bg = null;
        org.eclipse.collections.api.set.MutableSet<FaceAttribute> attrs = Sets.mutable.empty();

        int start = 0;
        while (start < body.length()) {
            int sep = body.indexOf(':', start);
            if (sep < 0) {
                sep = body.length();
            }
            String part = body.substring(start, sep);
            start = sep + 1;

            if (part.startsWith("fg=")) {
                fg = part.substring(3);
            } else if (part.startsWith("bg=")) {
                bg = part.substring(3);
            } else if ("bold".equals(part)) {
                attrs.add(FaceAttribute.BOLD);
            } else if ("underline".equals(part)) {
                attrs.add(FaceAttribute.UNDERLINE);
            }
        }

        return new FaceSpec(fg, bg, attrs.toImmutable());
    }
}
