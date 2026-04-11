package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * モード切り替え用コマンド。
 * モード登録時に自動生成される。
 */
public final class ModeCommand {

    private ModeCommand() {}

    /**
     * メジャーモードをアクティブバッファに設定するコマンド。
     * 設定後にモードフックを実行する。
     */
    public static final class SetMajorMode implements Command {

        private final String commandName;
        private final Supplier<MajorMode> factory;
        private final ModeRegistry modeRegistry;

        public SetMajorMode(String commandName, Supplier<MajorMode> factory, ModeRegistry modeRegistry) {
            this.commandName = commandName;
            this.factory = factory;
            this.modeRegistry = modeRegistry;
        }

        @Override
        public String name() {
            return commandName;
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            var mode = factory.get();
            var buffer = context.activeWindow().getBuffer();
            var oldMode = buffer.getMajorMode();
            modeRegistry.runMajorModeDisableHooks(oldMode.name(), buffer);
            oldMode.onDisable(buffer);
            buffer.setMajorMode(mode);
            mode.onEnable(buffer);
            modeRegistry.runMajorModeHooks(mode.name(), buffer);
            context.messageBuffer().message(mode.name() + " mode");
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * マイナーモードをアクティブバッファでトグルするコマンド。
     * 有効なら無効に、無効なら有効にする。
     * 有効化時にモードフックを実行する。
     */
    public static final class ToggleMinorMode implements Command {

        private final String commandName;
        private final String modeName;
        private final Supplier<MinorMode> factory;
        private final ModeRegistry modeRegistry;

        public ToggleMinorMode(
                String commandName, String modeName, Supplier<MinorMode> factory, ModeRegistry modeRegistry) {
            this.commandName = commandName;
            this.modeName = modeName;
            this.factory = factory;
            this.modeRegistry = modeRegistry;
        }

        @Override
        public String name() {
            return commandName;
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            var buffer = context.activeWindow().getBuffer();
            boolean enabled = buffer.getMinorModes().anySatisfy(m -> m.name().equals(modeName));
            if (enabled) {
                var modeToDisable = buffer.getMinorModes().detect(m -> m.name().equals(modeName));
                modeRegistry.runMinorModeDisableHooks(modeName, buffer);
                if (modeToDisable != null) {
                    modeToDisable.onDisable(buffer);
                    buffer.disableMinorMode(modeToDisable);
                }
                context.messageBuffer().message(modeName + " mode disabled");
            } else {
                var mode = factory.get();
                buffer.enableMinorMode(mode);
                mode.onEnable(buffer);
                modeRegistry.runMinorModeHooks(modeName, buffer);
                context.messageBuffer().message(modeName + " mode enabled");
            }
            return CompletableFuture.completedFuture(null);
        }
    }
}
