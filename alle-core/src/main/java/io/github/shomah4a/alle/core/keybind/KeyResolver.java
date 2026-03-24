package io.github.shomah4a.alle.core.keybind;

import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

/**
 * 複数のKeymapを優先順位順に探索し、キーストロークに対応するエントリを解決する。
 * グローバルキーマップを保持し、バッファローカル・モードキーマップと組み合わせた
 * 4階層のキーマップ解決を行う。
 *
 * 解決優先順位:
 * 1. バッファローカルキーマップ（ミニバッファ用等）
 * 2. マイナーモードキーマップ（後から有効にしたものが優先）
 * 3. メジャーモードキーマップ
 * 4. グローバルキーマップ
 */
public class KeyResolver {

    private final MutableList<Keymap> keymaps;

    public KeyResolver() {
        this.keymaps = Lists.mutable.empty();
    }

    /**
     * グローバルKeymapを追加する。先に追加されたものほど優先度が高い。
     */
    public void addKeymap(Keymap keymap) {
        keymaps.add(keymap);
    }

    /**
     * グローバルキーマップのみでキーストロークを解決する。
     */
    public Optional<KeymapEntry> resolve(KeyStroke keyStroke) {
        return resolveFromKeymaps(keyStroke, keymaps);
    }

    /**
     * バッファのキーマップ情報を含めた4階層でキーストロークを解決する。
     *
     * @param keyStroke 解決対象のキーストローク
     * @param localKeymap バッファローカルキーマップ（なければempty）
     * @param minorModeKeymaps マイナーモードのキーマップリスト（優先順位順、先頭が最優先）
     * @param majorModeKeymap メジャーモードのキーマップ（なければempty）
     */
    public Optional<KeymapEntry> resolveWithBuffer(
            KeyStroke keyStroke,
            Optional<Keymap> localKeymap,
            ListIterable<Keymap> minorModeKeymaps,
            Optional<Keymap> majorModeKeymap) {

        // 1. バッファローカルキーマップ
        if (localKeymap.isPresent()) {
            var entry = localKeymap.get().lookup(keyStroke);
            if (entry.isPresent()) {
                return entry;
            }
        }

        // 2. マイナーモードキーマップ（先頭が最優先）
        for (int i = 0; i < minorModeKeymaps.size(); i++) {
            var entry = minorModeKeymaps.get(i).lookup(keyStroke);
            if (entry.isPresent()) {
                return entry;
            }
        }

        // 3. メジャーモードキーマップ
        if (majorModeKeymap.isPresent()) {
            var entry = majorModeKeymap.get().lookup(keyStroke);
            if (entry.isPresent()) {
                return entry;
            }
        }

        // 4. グローバルキーマップ
        return resolve(keyStroke);
    }

    private static Optional<KeymapEntry> resolveFromKeymaps(KeyStroke keyStroke, ListIterable<Keymap> keymaps) {
        for (Keymap keymap : keymaps) {
            var entry = keymap.lookup(keyStroke);
            if (entry.isPresent()) {
                return entry;
            }
        }
        return Optional.empty();
    }
}
