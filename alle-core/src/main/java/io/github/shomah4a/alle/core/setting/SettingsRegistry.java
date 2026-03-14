package io.github.shomah4a.alle.core.setting;

import java.util.Optional;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * 設定定義の登録とグローバルデフォルト値の管理を行うレジストリ。
 * Emacsのdefvar/defcustomに相当する。
 */
public class SettingsRegistry {

    private final MutableMap<String, Setting<?>> definitions = Maps.mutable.empty();

    /**
     * 設定を登録する。
     *
     * @throws IllegalStateException 同名の設定が既に登録されている場合
     */
    public void register(Setting<?> setting) {
        String key = setting.key();
        if (definitions.containsKey(key)) {
            throw new IllegalStateException("設定 '" + key + "' は既に登録されています");
        }
        definitions.put(key, setting);
    }

    /**
     * 設定定義を名前で検索する。
     */
    public Optional<Setting<?>> lookup(String key) {
        return Optional.ofNullable(definitions.get(key));
    }

    /**
     * 登録済み設定名の一覧を返す。
     */
    public ImmutableSet<String> registeredKeys() {
        return definitions.keysView().toImmutableSet();
    }

    /**
     * 指定した設定のデフォルト値を返す。
     *
     * @throws IllegalArgumentException 未登録の設定の場合
     */
    public <T> T getDefault(Setting<T> setting) {
        if (!definitions.containsKey(setting.key())) {
            throw new IllegalArgumentException("設定 '" + setting.key() + "' は登録されていません");
        }
        return setting.defaultValue();
    }
}
