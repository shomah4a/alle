package io.github.shomah4a.alle.core.mode;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.ModeCommand;
import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.jspecify.annotations.Nullable;

/**
 * メジャーモード・マイナーモードを名前で登録・検索するレジストリ。
 * CommandRegistryが設定されている場合、モード登録時にモード切り替えコマンドも自動登録する。
 */
public class ModeRegistry {

    private final MutableMap<String, Supplier<MajorMode>> majorModes = Maps.mutable.empty();
    private final MutableMap<String, Supplier<MinorMode>> minorModes = Maps.mutable.empty();
    private @Nullable CommandRegistry commandRegistry;

    /**
     * コマンド自動登録先のCommandRegistryを設定する。
     */
    public void setCommandRegistry(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    /**
     * メジャーモードファクトリを登録する。
     *
     * @throws IllegalStateException 同名のモードが既に登録されている場合
     */
    public void registerMajorMode(String name, Supplier<MajorMode> factory) {
        if (majorModes.containsKey(name)) {
            throw new IllegalStateException("メジャーモード '" + name + "' は既に登録されています");
        }
        majorModes.put(name, factory);
        registerMajorModeCommand(name, factory);
    }

    /**
     * メジャーモードファクトリを登録する。同名のモードが既に登録されている場合は上書きする。
     * スクリプトからのモード再定義用。
     */
    public void registerOrReplaceMajorMode(String name, Supplier<MajorMode> factory) {
        majorModes.put(name, factory);
        registerMajorModeCommand(name, factory);
    }

    /**
     * 名前でメジャーモードファクトリを検索する。
     */
    public Optional<Supplier<MajorMode>> lookupMajorMode(String name) {
        return Optional.ofNullable(majorModes.get(name));
    }

    /**
     * 登録済みメジャーモード名の一覧を返す。
     */
    public ImmutableSet<String> registeredMajorModeNames() {
        return majorModes.keysView().toImmutableSet();
    }

    /**
     * マイナーモードファクトリを登録する。
     *
     * @throws IllegalStateException 同名のモードが既に登録されている場合
     */
    public void registerMinorMode(String name, Supplier<MinorMode> factory) {
        if (minorModes.containsKey(name)) {
            throw new IllegalStateException("マイナーモード '" + name + "' は既に登録されています");
        }
        minorModes.put(name, factory);
        registerMinorModeCommand(name, factory);
    }

    /**
     * マイナーモードファクトリを登録する。同名のモードが既に登録されている場合は上書きする。
     * スクリプトからのモード再定義用。
     */
    public void registerOrReplaceMinorMode(String name, Supplier<MinorMode> factory) {
        minorModes.put(name, factory);
        registerMinorModeCommand(name, factory);
    }

    /**
     * 名前でマイナーモードファクトリを検索する。
     */
    public Optional<Supplier<MinorMode>> lookupMinorMode(String name) {
        return Optional.ofNullable(minorModes.get(name));
    }

    /**
     * 登録済みマイナーモード名の一覧を返す。
     */
    public ImmutableSet<String> registeredMinorModeNames() {
        return minorModes.keysView().toImmutableSet();
    }

    private void registerMajorModeCommand(String modeName, Supplier<MajorMode> factory) {
        if (commandRegistry == null) {
            return;
        }
        String commandName = toCommandName(modeName);
        commandRegistry.registerOrReplace(new ModeCommand.SetMajorMode(commandName, factory));
    }

    private void registerMinorModeCommand(String modeName, Supplier<MinorMode> factory) {
        if (commandRegistry == null) {
            return;
        }
        String commandName = toCommandName(modeName);
        commandRegistry.registerOrReplace(new ModeCommand.ToggleMinorMode(commandName, modeName, factory));
    }

    /**
     * モード名をコマンド名に変換する。
     * CamelCase を kebab-case に変換し、末尾に "-mode" を付与する。
     * 例: "Python" → "python-mode", "ElectricPair" → "electric-pair-mode"
     */
    static String toCommandName(String modeName) {
        var sb = new StringBuilder();
        for (int i = 0; i < modeName.length(); i++) {
            char ch = modeName.charAt(i);
            if (Character.isUpperCase(ch) && i > 0) {
                sb.append('-');
            }
            sb.append(Character.toLowerCase(ch));
        }
        sb.append("-mode");
        return sb.toString();
    }
}
