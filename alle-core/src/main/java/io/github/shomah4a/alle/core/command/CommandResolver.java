package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.mode.MinorMode;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * コマンドの階層的名前解決を行う。
 * FQCN（mode-name.command-name）、MinorMode（後勝ち）、MajorMode、グローバルの順で検索する。
 * グローバルコマンドは "global.command-name" でも明示的にアクセスできる。
 * FQCNによるアクセスは登録済みの全モードに対して行える。
 */
public class CommandResolver {

    private static final String SEPARATOR = ".";
    private static final String GLOBAL_PREFIX = "global";

    private CommandRegistry globalRegistry;
    private final MutableMap<String, CommandRegistry> modeRegistries = Maps.mutable.empty();

    public CommandResolver(CommandRegistry globalRegistry) {
        this.globalRegistry = globalRegistry;
    }

    public CommandResolver() {
        this.globalRegistry = new CommandRegistry();
    }

    /**
     * グローバルレジストリを設定する。
     * 初期化順序の都合で後から設定する場合に使用する。
     */
    public void setGlobalRegistry(CommandRegistry globalRegistry) {
        this.globalRegistry = globalRegistry;
    }

    /**
     * モード名とそのCommandRegistryを登録する。
     * FQCNでのアクセスおよびM-x補完候補の列挙に使用される。
     */
    public void registerModeCommands(String modeName, CommandRegistry registry) {
        modeRegistries.put(modeName, registry);
    }

    /**
     * グローバルレジストリのみでコマンドを解決する。
     * バッファコンテキストが不要な場合に使用する。
     */
    public Optional<Command> resolve(String name) {
        if (name.contains(SEPARATOR)) {
            return resolveFqcn(name);
        }
        return globalRegistry.lookup(name);
    }

    /**
     * バッファのモードスコープを考慮してコマンドを解決する。
     * 優先順位: FQCN > MinorMode（後勝ち） > MajorMode > グローバル
     */
    public Optional<Command> resolve(String name, BufferFacade buffer) {
        // 1. FQCN
        if (name.contains(SEPARATOR)) {
            return resolveFqcn(name);
        }

        // 2. MinorMode（後勝ち）
        var minorModes = buffer.getMinorModes();
        for (int i = minorModes.size() - 1; i >= 0; i--) {
            var registryOpt = minorModes.get(i).commandRegistry();
            if (registryOpt.isPresent()) {
                var command = registryOpt.get().lookup(name);
                if (command.isPresent()) {
                    return command;
                }
            }
        }

        // 3. MajorMode
        var majorRegistryOpt = buffer.getMajorMode().commandRegistry();
        if (majorRegistryOpt.isPresent()) {
            var command = majorRegistryOpt.get().lookup(name);
            if (command.isPresent()) {
                return command;
            }
        }

        // 4. グローバル
        return globalRegistry.lookup(name);
    }

    /**
     * M-x補完用の全コマンド名を返す。
     * カレントバッファのモードに属するコマンドは短い名前で返す。
     * 登録済みの全モードのモード名プレフィックス（例: "Python."）を返す。
     * global.プレフィックスも返す。
     * FQCN付きのコマンド名はプレフィックス入力後の補完で返す（{@link #completeFqcn}）。
     */
    public ImmutableSet<String> allCommandNames(BufferFacade buffer) {
        MutableList<String> names = Lists.mutable.empty();

        // グローバルコマンド（短い名前）
        names.addAllIterable(globalRegistry.registeredNames());

        // カレントバッファのMajorModeコマンド（短い名前）
        var majorMode = buffer.getMajorMode();
        var majorRegistryOpt = majorMode.commandRegistry();
        if (majorRegistryOpt.isPresent()) {
            names.addAllIterable(majorRegistryOpt.get().registeredNames());
        }

        // カレントバッファのMinorModeコマンド（短い名前）
        for (MinorMode minorMode : buffer.getMinorModes()) {
            var registryOpt = minorMode.commandRegistry();
            if (registryOpt.isPresent()) {
                names.addAllIterable(registryOpt.get().registeredNames());
            }
        }

        // 登録済み全モードのモード名プレフィックス
        for (var entry : modeRegistries.keyValuesView()) {
            String modeName = entry.getOne();
            CommandRegistry registry = entry.getTwo();
            if (registry.registeredNames().notEmpty()) {
                names.add(modeName + SEPARATOR);
            }
        }

        // global.プレフィックス
        names.add(GLOBAL_PREFIX + SEPARATOR);

        return names.toImmutableSet();
    }

    /**
     * モード名プレフィックス入力後のFQCN補完候補を返す。
     * 入力が "モード名." で始まる場合、そのモードのコマンドをFQCN形式で返す。
     */
    public ImmutableSet<String> completeFqcn(String prefix) {
        int dotIndex = prefix.indexOf(SEPARATOR);
        if (dotIndex < 0) {
            return org.eclipse.collections.api.factory.Sets.immutable.empty();
        }
        String modeName = prefix.substring(0, dotIndex);
        String partialCommand = prefix.substring(dotIndex + 1);
        String modePrefix = modeName + SEPARATOR;

        MutableList<String> names = Lists.mutable.empty();

        if (GLOBAL_PREFIX.equals(modeName)) {
            for (String cmdName : globalRegistry.registeredNames()) {
                if (cmdName.startsWith(partialCommand)) {
                    names.add(modePrefix + cmdName);
                }
            }
        } else {
            CommandRegistry registry = modeRegistries.get(modeName);
            if (registry != null) {
                for (String cmdName : registry.registeredNames()) {
                    if (cmdName.startsWith(partialCommand)) {
                        names.add(modePrefix + cmdName);
                    }
                }
            }
        }

        return names.toImmutableSet();
    }

    /**
     * グローバルレジストリを返す。
     */
    public CommandRegistry globalRegistry() {
        return globalRegistry;
    }

    private Optional<Command> resolveFqcn(String fqcn) {
        int dotIndex = fqcn.indexOf(SEPARATOR);
        if (dotIndex < 0) {
            return Optional.empty();
        }
        String modePrefix = fqcn.substring(0, dotIndex);
        String commandName = fqcn.substring(dotIndex + 1);

        // global.command-name
        if (GLOBAL_PREFIX.equals(modePrefix)) {
            return globalRegistry.lookup(commandName);
        }

        // 登録済みモードから検索
        CommandRegistry modeRegistry = modeRegistries.get(modePrefix);
        if (modeRegistry != null) {
            return modeRegistry.lookup(commandName);
        }

        return Optional.empty();
    }
}
