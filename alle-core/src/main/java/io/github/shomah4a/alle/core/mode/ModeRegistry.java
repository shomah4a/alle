package io.github.shomah4a.alle.core.mode;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.ModeCommand;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.jspecify.annotations.Nullable;

/**
 * メジャーモード・マイナーモードを名前で登録・検索するレジストリ。
 * CommandRegistryが設定されている場合、モード登録時にモード切り替えコマンドも自動登録する。
 * モード有効化時のフック関数も管理する。
 */
public class ModeRegistry {

    private static final Logger logger = Logger.getLogger(ModeRegistry.class.getName());

    private final MutableMap<String, Supplier<MajorMode>> majorModes = Maps.mutable.empty();
    private final MutableMap<String, Supplier<MinorMode>> minorModes = Maps.mutable.empty();
    private final MutableMap<String, MutableList<BiConsumer<BufferFacade, String>>> majorModeHooks =
            Maps.mutable.empty();
    private final MutableMap<String, MutableList<BiConsumer<BufferFacade, String>>> minorModeHooks =
            Maps.mutable.empty();
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

    // ── フック管理 ──

    /**
     * メジャーモード有効化時のフックを追加する。
     * モード登録の有無に関係なく呼べる。
     * フック関数は (BufferFacade buffer, String modeName) を受け取る。
     *
     * @param modeName フックを紐付けるモード名
     * @param hook 有効化時に実行される関数
     */
    public void addMajorModeHook(String modeName, BiConsumer<BufferFacade, String> hook) {
        majorModeHooks.getIfAbsentPut(modeName, Lists.mutable::empty).add(hook);
    }

    /**
     * マイナーモード有効化時のフックを追加する。
     * モード登録の有無に関係なく呼べる。
     * フック関数は (BufferFacade buffer, String modeName) を受け取る。
     *
     * @param modeName フックを紐付けるモード名
     * @param hook 有効化時に実行される関数
     */
    public void addMinorModeHook(String modeName, BiConsumer<BufferFacade, String> hook) {
        minorModeHooks.getIfAbsentPut(modeName, Lists.mutable::empty).add(hook);
    }

    /**
     * メジャーモード有効化時のフックを実行する。
     * 各フックは try-catch で保護され、例外が発生しても残りのフックは継続実行される。
     *
     * @param modeName 有効化されたモードの名前
     * @param buffer 対象バッファ
     */
    public void runMajorModeHooks(String modeName, BufferFacade buffer) {
        var hooks = majorModeHooks.get(modeName);
        if (hooks != null) {
            runHooks(modeName, buffer, hooks);
        }
    }

    /**
     * マイナーモード有効化時のフックを実行する。
     * 各フックは try-catch で保護され、例外が発生しても残りのフックは継続実行される。
     *
     * @param modeName 有効化されたモードの名前
     * @param buffer 対象バッファ
     */
    public void runMinorModeHooks(String modeName, BufferFacade buffer) {
        var hooks = minorModeHooks.get(modeName);
        if (hooks != null) {
            runHooks(modeName, buffer, hooks);
        }
    }

    private void runHooks(String modeName, BufferFacade buffer, MutableList<BiConsumer<BufferFacade, String>> hooks) {
        for (var hook : hooks) {
            try {
                hook.accept(buffer, modeName);
            } catch (Exception e) {
                logger.log(Level.WARNING, modeName + " モードフック実行中にエラー", e);
            }
        }
    }

    // ── コマンド自動登録 ──

    private void registerMajorModeCommand(String modeName, Supplier<MajorMode> factory) {
        if (commandRegistry == null) {
            return;
        }
        String commandName = toCommandName(modeName);
        commandRegistry.registerOrReplace(new ModeCommand.SetMajorMode(commandName, factory, this));
    }

    private void registerMinorModeCommand(String modeName, Supplier<MinorMode> factory) {
        if (commandRegistry == null) {
            return;
        }
        String commandName = toCommandName(modeName);
        commandRegistry.registerOrReplace(new ModeCommand.ToggleMinorMode(commandName, modeName, factory, this));
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
