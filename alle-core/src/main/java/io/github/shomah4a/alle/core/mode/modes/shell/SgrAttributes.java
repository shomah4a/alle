package io.github.shomah4a.alle.core.mode.modes.shell;

import io.github.shomah4a.alle.core.styling.FaceName;
import org.jspecify.annotations.Nullable;

/**
 * ANSI SGR (Select Graphic Rendition) の属性を保持するイミュータブルクラス。
 * SGR コードを適用するたびに新しいインスタンスを返す。
 *
 * <p>{@link #toFaceName()} で生成される FaceName は {@code "ansi-sgr:"} プレフィックスの
 * 規約文字列を持ち、{@code DefaultFaceTheme} のフォールバック解決で FaceSpec に変換される。
 *
 * <p>このクラスはシェルモード内部でのみ使用する。
 */
final class SgrAttributes {

    private static final SgrAttributes DEFAULT = new SgrAttributes(null, null, false, false);

    private final @Nullable String foreground;
    private final @Nullable String background;
    private final boolean bold;
    private final boolean underline;

    private SgrAttributes(@Nullable String foreground, @Nullable String background, boolean bold, boolean underline) {
        this.foreground = foreground;
        this.background = background;
        this.bold = bold;
        this.underline = underline;
    }

    static SgrAttributes reset() {
        return DEFAULT;
    }

    /**
     * SGR コードを適用して新しいインスタンスを返す。
     */
    SgrAttributes withSgrCode(int code) {
        return switch (code) {
            case 0 -> DEFAULT;
            case 1 -> new SgrAttributes(foreground, background, true, underline);
            case 4 -> new SgrAttributes(foreground, background, bold, true);
            case 22 -> new SgrAttributes(foreground, background, false, underline);
            case 24 -> new SgrAttributes(foreground, background, bold, false);
            case 30 -> withForeground("black");
            case 31 -> withForeground("red");
            case 32 -> withForeground("green");
            case 33 -> withForeground("yellow");
            case 34 -> withForeground("blue");
            case 35 -> withForeground("magenta");
            case 36 -> withForeground("cyan");
            case 37 -> withForeground("white");
            case 39 -> withForeground(null);
            case 40 -> withBackground("black");
            case 41 -> withBackground("red");
            case 42 -> withBackground("green");
            case 43 -> withBackground("yellow");
            case 44 -> withBackground("blue");
            case 45 -> withBackground("magenta");
            case 46 -> withBackground("cyan");
            case 47 -> withBackground("white");
            case 49 -> withBackground(null);
            case 90 -> withForeground("black_bright");
            case 91 -> withForeground("red_bright");
            case 92 -> withForeground("green_bright");
            case 93 -> withForeground("yellow_bright");
            case 94 -> withForeground("blue_bright");
            case 95 -> withForeground("magenta_bright");
            case 96 -> withForeground("cyan_bright");
            case 97 -> withForeground("white_bright");
            case 100 -> withBackground("black_bright");
            case 101 -> withBackground("red_bright");
            case 102 -> withBackground("green_bright");
            case 103 -> withBackground("yellow_bright");
            case 104 -> withBackground("blue_bright");
            case 105 -> withBackground("magenta_bright");
            case 106 -> withBackground("cyan_bright");
            case 107 -> withBackground("white_bright");
            default -> this;
        };
    }

    /**
     * 属性がすべてデフォルトであるかを返す。
     */
    boolean isDefault() {
        return foreground == null && background == null && !bold && !underline;
    }

    /**
     * 現在の属性を表す FaceName を返す。
     * 属性がすべてデフォルトの場合は {@code null} を返す。
     *
     * <p>生成される名前の例: {@code "ansi-sgr:fg=red:bold"}, {@code "ansi-sgr:fg=green:bg=black"}
     */
    @Nullable
    FaceName toFaceName() {
        if (isDefault()) {
            return null;
        }
        var sb = new StringBuilder("ansi-sgr:");
        boolean first = true;
        if (foreground != null) {
            sb.append("fg=").append(foreground);
            first = false;
        }
        if (background != null) {
            if (!first) {
                sb.append(':');
            }
            sb.append("bg=").append(background);
            first = false;
        }
        if (bold) {
            if (!first) {
                sb.append(':');
            }
            sb.append("bold");
            first = false;
        }
        if (underline) {
            if (!first) {
                sb.append(':');
            }
            sb.append("underline");
        }
        return new FaceName(sb.toString(), "ANSI SGR 属性");
    }

    private SgrAttributes withForeground(@Nullable String fg) {
        return new SgrAttributes(fg, background, bold, underline);
    }

    private SgrAttributes withBackground(@Nullable String bg) {
        return new SgrAttributes(foreground, bg, bold, underline);
    }
}
