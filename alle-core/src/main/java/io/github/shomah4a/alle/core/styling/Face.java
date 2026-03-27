package io.github.shomah4a.alle.core.styling;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * テキストの見た目を定義する。
 * 色はNamed色名のみとし、具体的な色への解決はtui層のFaceResolverが行う。
 *
 * @param foreground 前景色名（"red", "blue", "default"等）
 * @param background 背景色名
 * @param attributes 装飾属性（BOLD, ITALIC, UNDERLINE）
 */
public record Face(String foreground, String background, ImmutableSet<FaceAttribute> attributes) {

    public static final Face DEFAULT = new Face("default", "default", Sets.immutable.empty());
    public static final Face HEADING = new Face("yellow", "default", Sets.immutable.of(FaceAttribute.BOLD));
    public static final Face BOLD_FACE = new Face("default", "default", Sets.immutable.of(FaceAttribute.BOLD));
    public static final Face ITALIC_FACE = new Face("default", "default", Sets.immutable.of(FaceAttribute.ITALIC));
    public static final Face CODE = new Face("green", "default", Sets.immutable.empty());
    public static final Face LINK = new Face("cyan", "default", Sets.immutable.of(FaceAttribute.UNDERLINE));
    public static final Face LIST_MARKER = new Face("magenta", "default", Sets.immutable.empty());
    public static final Face COMMENT = new Face("black_bright", "default", Sets.immutable.empty());
    public static final Face KEYWORD = new Face("blue", "default", Sets.immutable.of(FaceAttribute.BOLD));
    public static final Face STRING = new Face("green", "default", Sets.immutable.empty());
    public static final Face TABLE = new Face("cyan", "default", Sets.immutable.empty());
    public static final Face STRIKETHROUGH_FACE =
            new Face("default", "default", Sets.immutable.of(FaceAttribute.STRIKETHROUGH));
    public static final Face MINIBUFFER_PROMPT = new Face("cyan", "default", Sets.immutable.of(FaceAttribute.BOLD));

    /**
     * 前景色のみ指定してFaceを生成する。
     */
    public static Face of(String foreground) {
        return new Face(foreground, "default", Sets.immutable.empty());
    }

    /**
     * 前景色と装飾属性を指定してFaceを生成する。
     */
    public static Face of(String foreground, FaceAttribute... attrs) {
        return new Face(foreground, "default", Sets.immutable.with(attrs));
    }
}
