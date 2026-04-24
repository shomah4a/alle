package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.command.RectangleKillRing;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Test;

class KillRectangleCommandTest {

    @Test
    void コマンド名はkillRectangleである() {
        assertEquals("kill-rectangle", new KillRectangleCommand(new RectangleKillRing()).name());
    }

    @Test
    void 矩形範囲を削除してRingに保存する() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("foo\nbar\nbaz\n");
        window.setMark(0);
        window.setPoint(10);

        new KillRectangleCommand(ring).execute(context).join();

        assertEquals("o\nr\nz\n", window.getBuffer().getText());
        assertEquals(Lists.immutable.of("fo", "ba", "ba"), ring.current().orElseThrow());
    }

    @Test
    void killRectangle後のpointは矩形左上に移動する() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("foo\nbar\nbaz\n");
        window.setMark(0); // 行0 col 0
        window.setPoint(10); // 行2 col 2 (z の位置)

        new KillRectangleCommand(ring).execute(context).join();

        // 削除後 "o\nr\nz\n" (buffer len 6), point は矩形左上 (行0 col 0) = offset 0
        assertEquals(0, window.getPoint());
    }

    @Test
    void pointがmarkより前の場合も正しく動作する() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("foo\nbar\nbaz\n");
        window.setMark(10); // 行2 col 2
        window.setPoint(0); // 行0 col 0

        new KillRectangleCommand(ring).execute(context).join();

        assertEquals("o\nr\nz\n", window.getBuffer().getText());
        assertEquals(Lists.immutable.of("fo", "ba", "ba"), ring.current().orElseThrow());
    }

    @Test
    void 単一行の矩形も正しく動作する() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("hello world\n");
        window.setMark(6); // col 6 (w の前)
        window.setPoint(11); // col 11 (行末)

        new KillRectangleCommand(ring).execute(context).join();

        assertEquals("hello \n", window.getBuffer().getText());
        assertEquals(Lists.immutable.of("world"), ring.current().orElseThrow());
    }

    @Test
    void 末尾改行のないバッファでのkill() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("foo\nbar");
        window.setMark(0);
        window.setPoint(6); // 行1 col 2

        new KillRectangleCommand(ring).execute(context).join();

        assertEquals("o\nr", window.getBuffer().getText());
        assertEquals(Lists.immutable.of("fo", "ba"), ring.current().orElseThrow());
    }

    @Test
    void 矩形が行末を超える行を含むとき() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("long\nab\nxy\n");
        // 行0 "long" col 0, 行2 "xy" col 2 → 矩形 [0,2] × [0, 2)
        window.setMark(0);
        window.setPoint(10); // 行2 col 2 (y の後)

        new KillRectangleCommand(ring).execute(context).join();

        // 行0 "long" [0,2) → "lo"
        // 行1 "ab" [0,2) → "ab"
        // 行2 "xy" [0,2) → "xy"
        assertEquals(Lists.immutable.of("lo", "ab", "xy"), ring.current().orElseThrow());
        assertEquals("ng\n\n\n", window.getBuffer().getText());
    }

    @Test
    void 続けて再度killRectangleすると前のRing内容は上書きされる() {
        var ring = new RectangleKillRing();
        ring.put(Lists.immutable.of("OLD"));

        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("foo\nbar\n");
        window.setMark(0);
        window.setPoint(6); // 行1 col 2

        new KillRectangleCommand(ring).execute(context).join();

        assertEquals(Lists.immutable.of("fo", "ba"), ring.current().orElseThrow());
    }

    @Test
    void 幅0の矩形はno_op() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("a\tb\n");
        window.setMark(3); // 行0 col 3 (タブ中央)
        window.setPoint(3); // 同じ位置 → 幅 0

        new KillRectangleCommand(ring).execute(context).join();

        // 幅 0 は no-op。バッファ不変、Ring 未更新
        assertEquals("a\tb\n", window.getBuffer().getText());
        // Ring には何も put されない
        // (テスト簡潔化のため ring.current() の有無は検証しない。
        //  ring に何かあっても「前の値」なので、ここでは Ring が空であることを確認)
        org.junit.jupiter.api.Assertions.assertTrue(ring.current().isEmpty());
    }

    // === マルチバイト境界（全角・タブ）===

    @Test
    void 全角文字が右境界を跨ぐ場合は全角ごと含めてkillする() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        // "aあb" の "aあb\n" のみのバッファ
        window.insert("aあb\n");
        window.setMark(0); // 行0 col 0
        window.setPoint(2); // 行0 col 3 (cp 2 は b の前 = col 3)

        new KillRectangleCommand(ring).execute(context).join();

        // 矩形 [0, 0] × col[0, 3) → 右境界 col 3 は b の手前（境界上）
        // 切り出し: "aあ" (cp 0-2)、削除後 "b\n"
        assertEquals("b\n", window.getBuffer().getText());
        assertEquals(Lists.immutable.of("aあ"), ring.current().orElseThrow());
    }

    @Test
    void 右境界が全角中央に落ちる場合は全角ごとkill() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("aあb\n");
        // col[0, 2): 右境界 col 2 は あの中央 → 広げて col 3 (あの後ろ) → "aあ"
        // setMark/setPoint では中央カラムを指せないので Rectangle を直接構築
        var rect = new Rectangle(0, 0, 0, 2);
        var lines = RectangleGeometry.extractRectangle(window.getBuffer(), rect, 8);
        // col[0, 2): 右境界 col 2 は あ の中央 → 広げて col 3 (あの後ろ) → "aあ"
        assertEquals(Lists.immutable.of("aあ"), lines);

        // deleteRectangle も同様
        RectangleGeometry.deleteRectangle(window.getBuffer(), rect, 8);
        assertEquals("b\n", window.getBuffer().getText());
    }

    @Test
    void 左境界が全角中央に落ちる場合は全角ごとkill() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("aあb\n");
        // col[2, 4): 左境界 col 2 はあの中央 → 広げて col 1 (あの前) → "あb" (col 1-4)
        var rect = new Rectangle(0, 0, 2, 4);
        var lines = RectangleGeometry.extractRectangle(window.getBuffer(), rect, 8);
        assertEquals(Lists.immutable.of("あb"), lines);

        RectangleGeometry.deleteRectangle(window.getBuffer(), rect, 8);
        assertEquals("a\n", window.getBuffer().getText());
    }

    @Test
    void タブが右境界を跨ぐ場合はタブごと含めてkill() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        // "a\tb": a(col 0) \t(col 1-7) b(col 8)
        window.insert("a\tb\n");
        // col[0, 3): 右境界 col 3 はタブ中央 → 広げて col 8 (タブの後ろ) → "a\t"
        var rect = new Rectangle(0, 0, 0, 3);
        var lines = RectangleGeometry.extractRectangle(window.getBuffer(), rect, 8);
        assertEquals(Lists.immutable.of("a\t"), lines);

        RectangleGeometry.deleteRectangle(window.getBuffer(), rect, 8);
        // 削除後: "b\n" (a とタブが消え b だけ残る)
        assertEquals("b\n", window.getBuffer().getText());
    }

    @Test
    void 左境界がタブ中央に落ちる場合はタブごとkill() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("a\tb\n");
        // col[3, 9): 左境界 col 3 はタブ中央 → 広げて col 1 (タブの前) → "\tb"
        var rect = new Rectangle(0, 0, 3, 9);
        var lines = RectangleGeometry.extractRectangle(window.getBuffer(), rect, 8);
        assertEquals(Lists.immutable.of("\tb"), lines);

        RectangleGeometry.deleteRectangle(window.getBuffer(), rect, 8);
        assertEquals("a\n", window.getBuffer().getText());
    }

    @Test
    void 幅0でもタブ中央は跨ぎ文字を含まずno_op() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("a\tb\n");
        window.setMark(1); // col 1 (タブの前)
        window.setPoint(1); // 同じ位置 → 幅 0

        new KillRectangleCommand(ring).execute(context).join();

        // 幅 0 は no-op (タブも破壊されない)
        assertEquals("a\tb\n", window.getBuffer().getText());
    }

    @Test
    void killRectangleは1つのundo単位にまとまる() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        var buffer = window.getBuffer();
        window.insert("foo\nbar\nbaz\n");
        window.setMark(0);
        window.setPoint(10);

        int sizeBefore = buffer.getUndoManager().undoSize();

        new KillRectangleCommand(ring).execute(context).join();

        int sizeAfter = buffer.getUndoManager().undoSize();
        org.junit.jupiter.api.Assertions.assertEquals(
                sizeBefore + 1, sizeAfter, "kill-rectangle は 1 undo 単位にまとまる必要がある");

        // 実際に 1 回の undo で復元できる
        new UndoCommand().execute(context).join();
        assertEquals("foo\nbar\nbaz\n", buffer.getText());
    }

    @Test
    void killRectangleはCommandLoop経由でも1undoで戻る() throws Exception {
        // CommandLoop を介した実機に近い流れで undo 数を検証する。
        var settings = new io.github.shomah4a.alle.core.setting.SettingsRegistry();
        var textBuffer = new io.github.shomah4a.alle.core.buffer.TextBuffer(
                "test", new io.github.shomah4a.alle.core.textmodel.GapTextModel(), settings);
        var bufferFacade = new io.github.shomah4a.alle.core.buffer.BufferFacade(textBuffer);
        var window = new io.github.shomah4a.alle.core.window.Window(bufferFacade);
        var minibuffer = new io.github.shomah4a.alle.core.window.Window(
                new io.github.shomah4a.alle.core.buffer.BufferFacade(new io.github.shomah4a.alle.core.buffer.TextBuffer(
                        "*Minibuffer*", new io.github.shomah4a.alle.core.textmodel.GapTextModel(), settings)));
        var frame = new io.github.shomah4a.alle.core.window.Frame(window, minibuffer);
        var bufferManager = new io.github.shomah4a.alle.core.buffer.BufferManager();
        bufferManager.add(bufferFacade);

        var ring = new RectangleKillRing();
        var killCommand = new KillRectangleCommand(ring);
        var registry = new io.github.shomah4a.alle.core.command.CommandRegistry();
        registry.register(killCommand);

        // C-x r k プレフィックス構成を実機同様に組む
        var rMap = new io.github.shomah4a.alle.core.keybind.Keymap("C-x r");
        rMap.bind(io.github.shomah4a.alle.core.keybind.KeyStroke.of('k'), killCommand);
        var ctrlXMap = new io.github.shomah4a.alle.core.keybind.Keymap("C-x");
        ctrlXMap.bindPrefix(io.github.shomah4a.alle.core.keybind.KeyStroke.of('r'), rMap);
        var keymap = new io.github.shomah4a.alle.core.keybind.Keymap("global");
        keymap.bindPrefix(io.github.shomah4a.alle.core.keybind.KeyStroke.ctrl('x'), ctrlXMap);
        var keyResolver = new io.github.shomah4a.alle.core.keybind.KeyResolver();
        keyResolver.addKeymap(keymap);

        java.util.Iterator<io.github.shomah4a.alle.core.keybind.KeyStroke> it =
                java.util.Collections.<io.github.shomah4a.alle.core.keybind.KeyStroke>emptyList()
                        .iterator();
        io.github.shomah4a.alle.core.input.InputSource inputSource =
                () -> it.hasNext() ? java.util.Optional.of(it.next()) : java.util.Optional.empty();

        var loop = new io.github.shomah4a.alle.core.command.CommandLoop(
                inputSource,
                keyResolver,
                frame,
                bufferManager,
                (msg, hist) -> java.util.concurrent.CompletableFuture.completedFuture(
                        new io.github.shomah4a.alle.core.input.PromptResult.Cancelled()),
                new io.github.shomah4a.alle.core.command.KillRing(),
                new io.github.shomah4a.alle.core.buffer.MessageBuffer("*Messages*", 100, settings),
                new io.github.shomah4a.alle.core.buffer.MessageBuffer("*Warnings*", 100, settings),
                settings,
                new io.github.shomah4a.alle.core.command.CommandResolver(registry),
                new io.github.shomah4a.alle.core.io.PathOpenService(
                        new io.github.shomah4a.alle.core.io.BufferIO(
                                source -> {
                                    throw new java.io.IOException("stub");
                                },
                                destination -> {
                                    throw new java.io.IOException("stub");
                                },
                                settings),
                        new io.github.shomah4a.alle.core.mode.AutoModeMap(
                                io.github.shomah4a.alle.core.mode.modes.text.TextMode::new),
                        new io.github.shomah4a.alle.core.mode.ModeRegistry(),
                        settings,
                        path -> false,
                        (pathString, bufferManager2, frame2) -> {}));

        window.insert("foo\nbar\nbaz\n");
        window.setMark(0);
        window.setPoint(10);

        int sizeBefore = bufferFacade.getUndoManager().undoSize();
        // C-x r k の 3 段キー
        loop.processKey(io.github.shomah4a.alle.core.keybind.KeyStroke.ctrl('x'));
        loop.processKey(io.github.shomah4a.alle.core.keybind.KeyStroke.of('r'));
        loop.processKey(io.github.shomah4a.alle.core.keybind.KeyStroke.of('k'));
        int sizeAfter = bufferFacade.getUndoManager().undoSize();

        org.junit.jupiter.api.Assertions.assertEquals(
                sizeBefore + 1, sizeAfter, "CommandLoop 経由でも kill-rectangle は 1 undo にまとまる必要がある");
        assertEquals("o\nr\nz\n", bufferFacade.getText());
    }

    @Test
    void マルチバイト行とASCII行が混在する矩形() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("abcd\nあいう\nfoo\n");
        // "abcd" col 0-3, "あいう" col 0-5 (あ(0-1) い(2-3) う(4-5)), "foo" col 0-2
        // col[1, 3) の矩形
        // 行0 "abcd" [1, 3) → "bc"
        // 行1 "あいう" [1, 3): 左 col 1 はあの中央 → 広げて col 0 (あの前), 右 col 3 はいの後 → "あい"
        // 行2 "foo" [1, 3) → "oo"
        var rect = new Rectangle(0, 2, 1, 3);
        var lines = RectangleGeometry.extractRectangle(window.getBuffer(), rect, 8);
        assertEquals(Lists.immutable.of("bc", "あい", "oo"), lines);
    }
}
