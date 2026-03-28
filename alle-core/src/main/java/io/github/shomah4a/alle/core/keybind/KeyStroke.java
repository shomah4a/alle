package io.github.shomah4a.alle.core.keybind;

import java.util.Objects;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * 単一のキー入力。修飾キーの組み合わせとキーコードで構成される。
 * 例: Ctrl+X, Meta+F など。
 */
public record KeyStroke(ImmutableSet<Modifier> modifiers, int keyCode) {

    // 特殊キー用コードポイント（Unicode Private Use Area）
    public static final int ARROW_UP = 0xF700;
    public static final int ARROW_DOWN = 0xF701;
    public static final int ARROW_LEFT = 0xF702;
    public static final int ARROW_RIGHT = 0xF703;

    /**
     * 修飾キーなしのキーストロークを生成する。
     */
    public static KeyStroke of(int keyCode) {
        return new KeyStroke(Sets.immutable.empty(), keyCode);
    }

    /**
     * 修飾キー付きのキーストロークを生成する。
     */
    public static KeyStroke of(int keyCode, Modifier... modifiers) {
        return new KeyStroke(Sets.immutable.with(modifiers), keyCode);
    }

    /**
     * Ctrl+キーのキーストロークを生成する。
     */
    public static KeyStroke ctrl(int keyCode) {
        return new KeyStroke(Sets.immutable.of(Modifier.CTRL), keyCode);
    }

    /**
     * Meta+キーのキーストロークを生成する。
     */
    public static KeyStroke meta(int keyCode) {
        return new KeyStroke(Sets.immutable.of(Modifier.META), keyCode);
    }

    /**
     * Shift+キーのキーストロークを生成する。
     */
    public static KeyStroke shift(int keyCode) {
        return new KeyStroke(Sets.immutable.of(Modifier.SHIFT), keyCode);
    }

    /**
     * キーストロークをEmacs風の表示文字列に変換する。
     * 例: Ctrl+x → "C-x", Meta+x → "M-x", 通常文字 → "a"
     */
    public String displayString() {
        // Shift+Tab は Emacs に合わせて <backtab> と表示する
        if (keyCode == '\t' && modifiers.equals(Sets.immutable.of(Modifier.SHIFT))) {
            return "<backtab>";
        }
        var sb = new StringBuilder();
        if (modifiers.contains(Modifier.CTRL)) {
            sb.append("C-");
        }
        if (modifiers.contains(Modifier.META)) {
            sb.append("M-");
        }
        if (modifiers.contains(Modifier.SHIFT)) {
            sb.append("S-");
        }
        sb.append(keyCodeToString(keyCode));
        return sb.toString();
    }

    private static String keyCodeToString(int keyCode) {
        return switch (keyCode) {
            case ARROW_UP -> "<up>";
            case ARROW_DOWN -> "<down>";
            case ARROW_LEFT -> "<left>";
            case ARROW_RIGHT -> "<right>";
            case '\n' -> "RET";
            case ' ' -> "SPC";
            case 0x7F -> "DEL";
            case '\t' -> "TAB";
            case 0x1B -> "ESC";
            default -> {
                if (Character.isValidCodePoint(keyCode) && !Character.isISOControl(keyCode)) {
                    yield String.valueOf(Character.toChars(keyCode));
                }
                yield String.format("<%d>", keyCode);
            }
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof KeyStroke other)) return false;
        return keyCode == other.keyCode && modifiers.equals(other.modifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modifiers, keyCode);
    }
}
