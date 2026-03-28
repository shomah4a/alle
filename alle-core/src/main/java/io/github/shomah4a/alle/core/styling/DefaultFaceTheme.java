package io.github.shomah4a.alle.core.styling;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.ImmutableMap;

/**
 * 組み込みのデフォルトテーマ。
 * 現在のFace定数に相当するマッピングを提供する。
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
            .withKeyValue(FaceName.VARIABLE, FaceSpec.ofForeground("white"))
            .withKeyValue(FaceName.OPERATOR, FaceSpec.ofForeground("white"))
            .withKeyValue(FaceName.BUILTIN, FaceSpec.ofForeground("cyan"))
            .withKeyValue(FaceName.STRONG, FaceSpec.ofAttributes(FaceAttribute.BOLD))
            .withKeyValue(FaceName.EMPHASIS, FaceSpec.ofAttributes(FaceAttribute.ITALIC))
            .withKeyValue(FaceName.DELETION, FaceSpec.ofAttributes(FaceAttribute.STRIKETHROUGH))
            .withKeyValue(FaceName.MINIBUFFER_PROMPT, FaceSpec.of("cyan", FaceAttribute.BOLD))
            .toImmutable();

    @Override
    public FaceSpec resolve(FaceName name) {
        FaceSpec spec = MAPPING.get(name);
        return spec != null ? spec : DEFAULT_SPEC;
    }
}
