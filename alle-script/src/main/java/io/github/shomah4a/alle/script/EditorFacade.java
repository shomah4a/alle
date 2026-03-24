package io.github.shomah4a.alle.script;

import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.keybind.KeymapEntry;
import io.github.shomah4a.alle.core.window.FrameActor;
import java.util.List;
import java.util.Optional;
import org.graalvm.polyglot.Value;

/**
 * スクリプトに公開するエディタのルートファサード。
 * アクティブウィンドウ・バッファの解決、メッセージ表示、
 * コマンド登録・実行、キーバインド設定を担う。
 */
public class EditorFacade implements Loggable {

    private final FrameActor frameActor;
    private final MessageBuffer messageBuffer;
    private final CommandRegistry commandRegistry;
    private final Keymap globalKeymap;

    public EditorFacade(
            FrameActor frameActor, MessageBuffer messageBuffer, CommandRegistry commandRegistry, Keymap globalKeymap) {
        this.frameActor = frameActor;
        this.messageBuffer = messageBuffer;
        this.commandRegistry = commandRegistry;
        this.globalKeymap = globalKeymap;
    }

    /**
     * アクティブウィンドウのファサードを返す。
     */
    public WindowFacade activeWindow() {
        return new WindowFacade(frameActor.getActiveWindowActor());
    }

    /**
     * アクティブウィンドウのバッファのファサードを返す。
     */
    public BufferFacade currentBuffer() {
        return new BufferFacade(frameActor.getActiveWindowActor().getBufferActor());
    }

    /**
     * エコーエリアにメッセージを表示する。
     */
    public void message(String text) {
        messageBuffer.message(text);
    }

    /**
     * コマンドを登録する。同名のコマンドが既に存在する場合は上書きする。
     * Python側からAlleCommand（Java Commandを直接継承）が渡される。
     */
    public void registerCommand(Value command) {
        commandRegistry.registerOrReplace(command.as(Command.class));
    }

    /**
     * グローバルキーマップにキーバインドを設定する。
     * キーストロークのリストが複数要素の場合、プレフィックスキーを自動解決する。
     *
     * @param keyStrokes キーストロークのリスト（例: [ctrl('x'), ctrl('f')]）
     * @param commandValue バインドするコマンド
     */
    public void globalSetKey(List<KeyStroke> keyStrokes, Value commandValue) {
        Command command = commandValue.as(Command.class);
        if (keyStrokes.isEmpty()) {
            throw new IllegalArgumentException("キーストロークのリストが空です");
        }
        if (keyStrokes.size() == 1) {
            globalKeymap.bind(keyStrokes.get(0), command);
            return;
        }
        // プレフィックスキーの自動解決
        Keymap current = globalKeymap;
        for (int i = 0; i < keyStrokes.size() - 1; i++) {
            KeyStroke prefix = keyStrokes.get(i);
            Optional<KeymapEntry> entry = current.lookup(prefix);
            if (entry.isPresent() && entry.get() instanceof KeymapEntry.PrefixBinding pb) {
                current = pb.keymap();
            } else {
                var newMap = new Keymap(prefix.displayString());
                current.bindPrefix(prefix, newMap);
                current = newMap;
            }
        }
        current.bind(keyStrokes.get(keyStrokes.size() - 1), command);
    }
}
