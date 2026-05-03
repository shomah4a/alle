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
    private final MutableMap<String, Object> globalValues = Maps.mutable.empty();

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

    /**
     * ユーザーが明示的に設定したグローバル値を設定する。
     *
     * @throws IllegalArgumentException 未登録の設定の場合
     */
    public <T> void setGlobal(Setting<T> setting, T value) {
        if (!definitions.containsKey(setting.key())) {
            throw new IllegalArgumentException("設定 '" + setting.key() + "' は登録されていません");
        }
        globalValues.put(setting.key(), value);
    }

    /**
     * ユーザーが明示的に設定したグローバル値を取得する。
     * 設定されていない場合はemptyを返す。フォールバックは含まない。
     *
     * @throws IllegalArgumentException 未登録の設定の場合
     */
    public <T> Optional<T> getGlobal(Setting<T> setting) {
        if (!definitions.containsKey(setting.key())) {
            throw new IllegalArgumentException("設定 '" + setting.key() + "' は登録されていません");
        }
        Object value = globalValues.get(setting.key());
        if (value != null) {
            return Optional.of(setting.cast(value));
        }
        return Optional.empty();
    }

    /**
     * ユーザーグローバル値が設定されているかを返す。
     */
    public boolean hasGlobal(Setting<?> setting) {
        return globalValues.containsKey(setting.key());
    }

    /**
     * 設定の実効値を返す。
     * 設定が未登録、またはグローバル値が未設定の場合は {@link Setting#defaultValue()} を返す。
     *
     * <p>{@link #getGlobal(Setting)} は未登録時に例外を投げるが、こちらは投げない。
     * 設定の存在を前提にできない呼び出し側（コマンド実装等）から利用する。
     */
    public <T> T getEffective(Setting<T> setting) {
        if (!definitions.containsKey(setting.key())) {
            return setting.defaultValue();
        }
        Object value = globalValues.get(setting.key());
        if (value != null) {
            return setting.cast(value);
        }
        return setting.defaultValue();
    }

    /**
     * ユーザーグローバル値を解除し、Setting.defaultValueにフォールバックするようにする。
     */
    public void removeGlobal(Setting<?> setting) {
        globalValues.remove(setting.key());
    }
}
