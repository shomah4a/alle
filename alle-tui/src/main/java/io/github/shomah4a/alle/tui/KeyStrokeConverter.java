package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.input.KeyType;
import io.github.shomah4a.alle.core.keybind.Modifier;
import java.util.Optional;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;

/**
 * LanternaのKeyStrokeからalleのKeyStrokeへの変換を行う。
 * 変換不能なキー（未対応の特殊キー等）はemptyを返す。
 */
public final class KeyStrokeConverter {

    private KeyStrokeConverter() {}

    /**
     * LanternaのKeyStrokeをalleのKeyStrokeに変換する。
     * 変換不能な場合はemptyを返す。
     */
    public static Optional<io.github.shomah4a.alle.core.keybind.KeyStroke> convert(
            com.googlecode.lanterna.input.KeyStroke lanternaKeyStroke) {
        ImmutableSet<Modifier> modifiers = extractModifiers(lanternaKeyStroke);
        KeyType keyType = lanternaKeyStroke.getKeyType();

        return switch (keyType) {
            case Character -> {
                Character ch = lanternaKeyStroke.getCharacter();
                if (ch == null) {
                    yield Optional.empty();
                }
                int codePoint = ch.charValue();
                yield Optional.of(new io.github.shomah4a.alle.core.keybind.KeyStroke(modifiers, codePoint));
            }
            case Enter -> Optional.of(new io.github.shomah4a.alle.core.keybind.KeyStroke(modifiers, '\n'));
            case Backspace -> Optional.of(new io.github.shomah4a.alle.core.keybind.KeyStroke(modifiers, 0x7F));
            case ArrowUp ->
                Optional.of(new io.github.shomah4a.alle.core.keybind.KeyStroke(
                        modifiers, io.github.shomah4a.alle.core.keybind.KeyStroke.ARROW_UP));
            case ArrowDown ->
                Optional.of(new io.github.shomah4a.alle.core.keybind.KeyStroke(
                        modifiers, io.github.shomah4a.alle.core.keybind.KeyStroke.ARROW_DOWN));
            case ArrowLeft ->
                Optional.of(new io.github.shomah4a.alle.core.keybind.KeyStroke(
                        modifiers, io.github.shomah4a.alle.core.keybind.KeyStroke.ARROW_LEFT));
            case ArrowRight ->
                Optional.of(new io.github.shomah4a.alle.core.keybind.KeyStroke(
                        modifiers, io.github.shomah4a.alle.core.keybind.KeyStroke.ARROW_RIGHT));
            case EOF -> Optional.empty();
            default -> Optional.empty();
        };
    }

    private static ImmutableSet<Modifier> extractModifiers(com.googlecode.lanterna.input.KeyStroke keyStroke) {
        MutableSet<Modifier> modifiers = Sets.mutable.empty();
        if (keyStroke.isCtrlDown()) {
            modifiers.add(Modifier.CTRL);
        }
        if (keyStroke.isAltDown()) {
            modifiers.add(Modifier.META);
        }
        if (keyStroke.isShiftDown()) {
            modifiers.add(Modifier.SHIFT);
        }
        return modifiers.toImmutable();
    }
}
