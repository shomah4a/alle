package io.github.shomah4a.alle.core.mode.modes.shell;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
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
 * <p>呼び出すたびに新しいシェルバッファを作成する。
 * バッファ名は {@code *shell*}, {@code *shell*<2>}, {@code *shell*<3>}, ... と連番が付く。
 */
public class ShellCommand implements Command {

    private static final String BUFFER_NAME_BASE = "*shell*";

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
        String bufferName = resolveBufferName(context.bufferManager());

        var textBuffer = new TextBuffer(bufferName, new GapTextModel(), settingsRegistry);
        var buffer = new BufferFacade(textBuffer);

        Path workingDirectory = context.activeWindow()
                .getBuffer()
                .getDefaultDirectory(Path.of("").toAbsolutePath());

        // model を先に作成し、コールバックから直接参照できるようにする。
        // プロセスは model 作成後に起動・注入する。
        var window = context.activeWindow();
        var model = new ShellBufferModel(buffer, () -> {
            // 出力追記後にカーソルをバッファ末尾に移動
            if (window.getBuffer().equals(buffer)) {
                window.setPoint(buffer.length());
            }
        });

        var process = processFactory.create(workingDirectory, model::appendOutput, model::markProcessFinished);

        model.setProcess(process);

        var shellMode = new ShellMode(model, shellKeymap, shellCommandRegistry);
        buffer.setMajorMode(shellMode);

        context.bufferManager().add(buffer);
        context.activeWindow().setBuffer(buffer);
        context.activeWindow().setPoint(0);

        return CompletableFuture.completedFuture(null);
    }

    private static String resolveBufferName(BufferManager bufferManager) {
        if (bufferManager.findByName(BUFFER_NAME_BASE).isEmpty()) {
            return BUFFER_NAME_BASE;
        }
        for (int i = 2; ; i++) {
            String candidate = BUFFER_NAME_BASE + "<" + i + ">";
            if (bufferManager.findByName(candidate).isEmpty()) {
                return candidate;
            }
        }
    }
}
