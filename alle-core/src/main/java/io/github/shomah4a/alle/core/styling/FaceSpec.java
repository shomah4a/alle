package io.github.shomah4a.alle.core.styling;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;
import org.jspecify.annotations.Nullable;

/**
 * 視覚属性の部分定義。
 * foreground/backgroundがnullの場合は「未指定（下位レイヤに委ねる）」を意味する。
 *
 * @param foreground 前景色名（nullは未指定）
 * @param background 背景色名（nullは未指定）
 * @param attributes 装飾属性（空は属性指定なし）
 */
public record FaceSpec(
        @Nullable String foreground, @Nullable String background, ImmutableSet<FaceAttribute> attributes) {

    /**
     * 前景色のみ指定してFaceSpecを生成する。
     */
    public static FaceSpec ofForeground(String foreground) {
        return new FaceSpec(foreground, null, Sets.immutable.empty());
    }

    /**
     * 装飾属性のみ指定してFaceSpecを生成する。
     */
    public static FaceSpec ofAttributes(FaceAttribute... attrs) {
        return new FaceSpec(null, null, Sets.immutable.with(attrs));
    }

    /**
     * 前景色と装飾属性を指定してFaceSpecを生成する。
     */
    public static FaceSpec of(String foreground, FaceAttribute... attrs) {
        return new FaceSpec(foreground, null, Sets.immutable.with(attrs));
    }

    /**
     * 2つのFaceSpecを合成する。
     * 後勝ち: overlayのforeground/backgroundがnon-nullなら上書き、nullなら下位を維持。
     * attributesは和集合。
     */
    public FaceSpec merge(FaceSpec overlay) {
        String mergedFg = overlay.foreground != null ? overlay.foreground : this.foreground;
        String mergedBg = overlay.background != null ? overlay.background : this.background;
        ImmutableSet<FaceAttribute> mergedAttrs = this.attributes.union(overlay.attributes);
        return new FaceSpec(mergedFg, mergedBg, mergedAttrs);
    }
}
