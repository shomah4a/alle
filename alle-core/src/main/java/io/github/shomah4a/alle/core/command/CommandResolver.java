package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.mode.MinorMode;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * コマンドの階層的名前解決を行う。
 * FQCN（mode-name.command-name）、MinorMode（後勝ち）、MajorMode、グローバルの順で検索する。
 * グローバルコマンドは "global.command-name" でも明示的にアクセスできる。
 */
public class CommandResolver {

    private static final String SEPARATOR = ".";
    private static final String GLOBAL_PREFIX = "global";

    private final CommandRegistry globalRegistry;

    public CommandResolver(CommandRegistry globalRegistry) {
        this.globalRegistry = globalRegistry;
    }

    /**
     * グローバルレジストリのみでコマンドを解決する。
     * バッファコンテキストが不要な場合に使用する。
     */
    public Optional<Command> resolve(String name) {
        // FQCNの場合はglobalプレフィックスのみ対応
        if (name.contains(SEPARATOR)) {
            return resolveGlobalFqcn(name);
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
            return resolveFqcn(name, buffer);
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
     * バッファのモードスコープを考慮して、M-x補完用の全コマンド名を返す。
     * バッファのモードに属するコマンドは短い名前で返す。
     * コマンドを持つモードはモード名+ドットのプレフィックス候補も返す。
     * グローバルコマンドは短い名前で返す。
     */
    public ImmutableSet<String> allCommandNames(BufferFacade buffer) {
        MutableList<String> names = Lists.mutable.empty();

        // グローバルコマンド（短い名前）
        names.addAllIterable(globalRegistry.registeredNames());

        // MajorModeコマンド（短い名前 + FQCN + モード名プレフィックス候補）
        var majorMode = buffer.getMajorMode();
        var majorRegistryOpt = majorMode.commandRegistry();
        if (majorRegistryOpt.isPresent()) {
            var majorNames = majorRegistryOpt.get().registeredNames();
            names.addAllIterable(majorNames);
            String modePrefix = majorMode.name() + SEPARATOR;
            names.add(modePrefix);
            for (String cmdName : majorNames) {
                names.add(modePrefix + cmdName);
            }
        }

        // MinorModeコマンド（短い名前 + FQCN + モード名プレフィックス候補）
        for (MinorMode minorMode : buffer.getMinorModes()) {
            var registryOpt = minorMode.commandRegistry();
            if (registryOpt.isPresent()) {
                var minorNames = registryOpt.get().registeredNames();
                names.addAllIterable(minorNames);
                String modePrefix = minorMode.name() + SEPARATOR;
                names.add(modePrefix);
                for (String cmdName : minorNames) {
                    names.add(modePrefix + cmdName);
                }
            }
        }

        // global.プレフィックス候補
        names.add(GLOBAL_PREFIX + SEPARATOR);
        for (String cmdName : globalRegistry.registeredNames()) {
            names.add(GLOBAL_PREFIX + SEPARATOR + cmdName);
        }

        return names.toImmutableSet();
    }

    /**
     * グローバルレジストリを返す。
     */
    public CommandRegistry globalRegistry() {
        return globalRegistry;
    }

    private Optional<Command> resolveGlobalFqcn(String fqcn) {
        int dotIndex = fqcn.indexOf(SEPARATOR);
        if (dotIndex < 0) {
            return Optional.empty();
        }
        String modePrefix = fqcn.substring(0, dotIndex);
        String commandName = fqcn.substring(dotIndex + 1);

        if (GLOBAL_PREFIX.equals(modePrefix)) {
            return globalRegistry.lookup(commandName);
        }
        return Optional.empty();
    }

    private Optional<Command> resolveFqcn(String fqcn, BufferFacade buffer) {
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

        // MajorModeの名前で一致を検索
        var majorMode = buffer.getMajorMode();
        if (majorMode.name().equals(modePrefix)) {
            var registryOpt = majorMode.commandRegistry();
            if (registryOpt.isPresent()) {
                return registryOpt.get().lookup(commandName);
            }
        }

        // MinorModeの名前で一致を検索
        for (MinorMode minorMode : buffer.getMinorModes()) {
            if (minorMode.name().equals(modePrefix)) {
                var registryOpt = minorMode.commandRegistry();
                if (registryOpt.isPresent()) {
                    return registryOpt.get().lookup(commandName);
                }
            }
        }

        return Optional.empty();
    }
}
