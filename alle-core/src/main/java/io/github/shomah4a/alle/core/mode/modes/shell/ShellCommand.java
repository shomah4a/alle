package io.github.shomah4a.alle.core.mode.modes.shell;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * 対話的シェルバッファを開くコマンド。
 * Emacs の {@code M-x shell} に相当する。
 *
 * <p>{@code *shell*} バッファが既に存在する場合はそのバッファに切り替え、
 * 存在しない場合は新規作成してシェルプロセスを起動する。
 */
public class ShellCommand implements Command {

    private static final String BUFFER_NAME = "*shell*";

    private final ShellProcessFactory processFactory;
    private final Keymap shellKeymap;
    private final CommandRegistry shellCommandRegistry;
    private final SettingsRegistry settingsRegistry;

    ShellCommand(
            ShellProcessFactory processFactory,
            Keymap shellKeymap,
            CommandRegistry shellCommandRegistry,
            SettingsRegistry settingsRegistry) {
        this.processFactory = processFactory;
        this.shellKeymap = shellKeymap;
        this.shellCommandRegistry = shellCommandRegistry;
        this.settingsRegistry = settingsRegistry;
    }

    @Override
    public String name() {
        return "shell";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var existing = context.bufferManager().findByName(BUFFER_NAME);
        if (existing.isPresent()) {
            context.activeWindow().setBuffer(existing.get());
            context.activeWindow().setPoint(existing.get().length());
            return CompletableFuture.completedFuture(null);
        }

        var textBuffer = new TextBuffer(BUFFER_NAME, new GapTextModel(), settingsRegistry);
        var buffer = new BufferFacade(textBuffer);

        Path workingDirectory = context.activeWindow()
                .getBuffer()
                .getDefaultDirectory(Path.of("").toAbsolutePath());

        var process = processFactory.create(workingDirectory, line -> {
            var mode = buffer.getMajorMode();
            if (mode instanceof ShellMode shellMode) {
                shellMode.getModel().appendOutput(line);
            }
        });

        var model = new ShellBufferModel(buffer, process);
        var shellMode = new ShellMode(model, shellKeymap, shellCommandRegistry);
        buffer.setMajorMode(shellMode);

        context.bufferManager().add(buffer);
        context.activeWindow().setBuffer(buffer);
        context.activeWindow().setPoint(0);

        return CompletableFuture.completedFuture(null);
    }
}
