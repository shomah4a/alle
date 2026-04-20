package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.CommandResolver;
import io.github.shomah4a.alle.core.command.KillRing;
import io.github.shomah4a.alle.core.command.NoOpOverridingKeymapController;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Test;

class StringRectangleCommandTest {

    @Test
    void コマンド名はstringRectangleである() {
        assertEquals("string-rectangle", new StringRectangleCommand(new InputHistory()).name());
    }

    @Test
    void 矩形範囲を入力文字列で置き換える() {
        var context = fixedPromptContext("XY");
        var window = context.activeWindow();
        window.insert("abcd\nefgh\nijkl\n");
        window.setMark(0);
        window.setPoint(12); // 行2 col 2

        new StringRectangleCommand(new InputHistory()).execute(context).join();

        // 各行の col[0, 2) を "XY" で置き換え
        assertEquals("XYcd\nXYgh\nXYkl\n", window.getBuffer().getText());
    }

    @Test
    void 空文字列入力時は矩形を削除する() {
        var context = fixedPromptContext("");
        var window = context.activeWindow();
        window.insert("abcd\nefgh\nijkl\n");
        window.setMark(0);
        window.setPoint(12);

        new StringRectangleCommand(new InputHistory()).execute(context).join();

        // 空文字列なら delete-rectangle 相当
        assertEquals("cd\ngh\nkl\n", window.getBuffer().getText());
    }

    @Test
    void キャンセル時はバッファ不変() {
        var context = cancelPromptContext();
        var window = context.activeWindow();
        window.insert("abcd\nefgh\n");
        window.setMark(0);
        window.setPoint(6);

        new StringRectangleCommand(new InputHistory()).execute(context).join();

        assertEquals("abcd\nefgh\n", window.getBuffer().getText());
    }

    @Test
    void markが未設定の場合は何もしない() {
        var context = fixedPromptContext("XY");
        var window = context.activeWindow();
        window.insert("abcd\n");

        new StringRectangleCommand(new InputHistory()).execute(context).join();

        assertEquals("abcd\n", window.getBuffer().getText());
    }

    @Test
    void 入力カラム幅が矩形幅と異なってもpadding無しで置換される() {
        var context = fixedPromptContext("XYZ"); // 3 文字 vs 矩形幅 2
        var window = context.activeWindow();
        window.insert("abcd\nefgh\n");
        window.setMark(0);
        window.setPoint(7); // 行1 col 2

        new StringRectangleCommand(new InputHistory()).execute(context).join();

        // 各行の col[0, 2) を "XYZ"（3 文字）で置き換え → 幅が 1 増える
        assertEquals("XYZcd\nXYZgh\n", window.getBuffer().getText());
    }

    private static CommandContext fixedPromptContext(String response) {
        InputPrompter prompter = (msg, hist) -> CompletableFuture.completedFuture(new PromptResult.Confirmed(response));
        return buildContext(prompter);
    }

    private static CommandContext cancelPromptContext() {
        InputPrompter prompter = (msg, hist) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());
        return buildContext(prompter);
    }

    private static CommandContext buildContext(InputPrompter prompter) {
        var settings = new SettingsRegistry();
        var textBuffer = new TextBuffer("test", new GapTextModel(), settings);
        var bufferFacade = new BufferFacade(textBuffer);
        var window = new Window(bufferFacade);
        var minibuffer = new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), settings)));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(bufferFacade);
        return new CommandContext(
                frame,
                bufferManager,
                frame.getActiveWindow(),
                prompter,
                Lists.immutable.empty(),
                Optional.empty(),
                Optional.empty(),
                new KillRing(),
                new MessageBuffer("*Messages*", 100, settings),
                new MessageBuffer("*Warnings*", 100, settings),
                settings,
                new CommandResolver(new CommandRegistry()),
                new NoOpOverridingKeymapController());
    }
}
