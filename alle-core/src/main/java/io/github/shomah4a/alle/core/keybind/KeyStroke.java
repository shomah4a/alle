package io.github.shomah4a.alle.core.keybind;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * 単一のキー入力。修飾キーの組み合わせとキーコードで構成される。
 * 例: Ctrl+X, Meta+F など。
 */
public record KeyStroke(Set<Modifier> modifiers, int keyCode) {

    public KeyStroke {
        modifiers = modifiers.isEmpty() ? EnumSet.noneOf(Modifier.class) : EnumSet.copyOf(modifiers);
    }

    /**
     * 修飾キーなしのキーストロークを生成する。
     */
    public static KeyStroke of(int keyCode) {
        return new KeyStroke(EnumSet.noneOf(Modifier.class), keyCode);
    }

    /**
     * 修飾キー付きのキーストロークを生成する。
     */
    public static KeyStroke of(int keyCode, Modifier... modifiers) {
        return new KeyStroke(
                modifiers.length == 0 ? EnumSet.noneOf(Modifier.class) : EnumSet.of(modifiers[0], modifiers), keyCode);
    }

    /**
     * Ctrl+キーのキーストロークを生成する。
     */
    public static KeyStroke ctrl(int keyCode) {
        return new KeyStroke(EnumSet.of(Modifier.CTRL), keyCode);
    }

    /**
     * Meta+キーのキーストロークを生成する。
     */
    public static KeyStroke meta(int keyCode) {
        return new KeyStroke(EnumSet.of(Modifier.META), keyCode);
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
