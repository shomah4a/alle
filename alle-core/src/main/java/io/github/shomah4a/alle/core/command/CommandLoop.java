package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.input.InputSource;
import io.github.shomah4a.alle.core.keybind.KeyResolver;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.keybind.KeymapEntry;
import io.github.shomah4a.alle.core.window.Frame;
import java.util.Optional;

/**
 * キー入力→コマンド解決→コマンド実行のメインループ。
 * InputSourceからキーを読み、KeyResolverでコマンドを解決し、実行する。
 * キーマップに未マッチのキーは無視される。
 */
public class CommandLoop {

    private final InputSource inputSource;
    private final KeyResolver keyResolver;
    private final Frame frame;
    private final BufferManager bufferManager;
    private Optional<String> lastCommand = Optional.empty();

    public CommandLoop(InputSource inputSource, KeyResolver keyResolver, Frame frame, BufferManager bufferManager) {
        this.inputSource = inputSource;
        this.keyResolver = keyResolver;
        this.frame = frame;
        this.bufferManager = bufferManager;
    }

    /**
     * メインループを実行する。
     * InputSourceがemptyを返すと終了する。
     */
    public void run() {
        while (true) {
            var keyOpt = inputSource.readKeyStroke();
            if (keyOpt.isEmpty()) {
                break;
            }
            processKey(keyOpt.get());
        }
    }

    /**
     * 1つのキーストロークを処理する。テスト用に公開。
     */
    public void processKey(KeyStroke keyStroke) {
        var entryOpt = keyResolver.resolve(keyStroke);
        if (entryOpt.isPresent()) {
            handleEntry(entryOpt.get(), keyStroke);
        }
    }

    private void handleEntry(KeymapEntry entry, KeyStroke keyStroke) {
        switch (entry) {
            case KeymapEntry.CommandBinding(var command) -> {
                var thisCommand = Optional.of(command.name());
                var context =
                        new CommandContext(frame, bufferManager, Optional.of(keyStroke), thisCommand, lastCommand);
                command.execute(context);
                lastCommand = thisCommand;
            }
            case KeymapEntry.PrefixBinding(var prefixKeymap) -> handlePrefix(prefixKeymap);
        }
    }

    private void handlePrefix(Keymap prefixKeymap) {
        var nextKeyOpt = inputSource.readKeyStroke();
        if (nextKeyOpt.isEmpty()) {
            return;
        }
        var nextKey = nextKeyOpt.get();
        var entryOpt = prefixKeymap.lookup(nextKey);
        if (entryOpt.isPresent()) {
            handleEntry(entryOpt.get(), nextKey);
        }
    }
}
