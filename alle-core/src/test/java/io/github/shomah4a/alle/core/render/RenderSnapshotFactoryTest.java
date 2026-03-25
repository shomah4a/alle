package io.github.shomah4a.alle.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RenderSnapshotFactoryTest {

    private BufferFacade createBuffer(String name, String content) {
        var textModel = new GapTextModel();
        var buffer = new EditableBuffer(name, textModel);
        var facade = new BufferFacade(buffer);
        if (!content.isEmpty()) {
            facade.insertText(0, content);
        }
        return facade;
    }

    private Frame createFrame(String content) {
        var mainBuffer = createBuffer("main", content);
        var minibuffer = createBuffer("*Minibuffer*", "");
        return new Frame(new Window(mainBuffer), new Window(minibuffer));
    }

    private MessageBuffer createMessageBuffer() {
        return new MessageBuffer("*Messages*", 100);
    }

    @Nested
    class 基本的なスナップショット生成 {

        @Test
        void 空バッファから画面サイズに応じたスナップショットを生成する() {
            var frame = createFrame("");
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            assertEquals(80, snapshot.screenCols());
            assertEquals(24, snapshot.screenRows());
            assertEquals(1, snapshot.windowSnapshots().size());
        }

        @Test
        void 複数行バッファの可視行が正しく取得される() {
            var frame = createFrame("line1\nline2\nline3");
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            var ws = snapshot.windowSnapshots().get(0);
            assertEquals(3, ws.visibleLines().size());
            assertEquals("line1", ws.visibleLines().get(0).text());
            assertEquals("line2", ws.visibleLines().get(1).text());
            assertEquals("line3", ws.visibleLines().get(2).text());
        }

        @Test
        void 画面行数を超える行はスナップショットに含まれない() {
            var frame = createFrame("a\nb\nc\nd\ne");
            // rows=4 → ウィンドウ領域3行（モードライン1行分引く→バッファ表示2行）
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 4);

            var ws = snapshot.windowSnapshots().get(0);
            assertEquals(2, ws.visibleLines().size());
            assertEquals("a", ws.visibleLines().get(0).text());
            assertEquals("b", ws.visibleLines().get(1).text());
        }
    }

    @Nested
    class モードライン {

        @Test
        void バッファ名とモード名とカーソル位置を含む() {
            var frame = createFrame("hello\nworld");
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            var modeLine = snapshot.windowSnapshots().get(0).modeLine();
            assertTrue(modeLine.contains("main"), "バッファ名を含む");
            assertTrue(modeLine.contains("(1,0)"), "カーソル位置(行1,列0)を含む");
            assertTrue(modeLine.contains("Text"), "モード名を含む");
        }

        @Test
        void 未変更バッファのダーティフラグはハイフン表示() {
            var frame = createFrame("");
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            var modeLine = snapshot.windowSnapshots().get(0).modeLine();
            assertTrue(modeLine.startsWith("----"), "未変更バッファは--で始まる");
        }

        @Test
        void 変更済みバッファのダーティフラグはアスタリスク表示() {
            var mainBuffer = createBuffer("main", "hello");
            var facade = mainBuffer;
            facade.markDirty();
            var minibuffer = createBuffer("*Minibuffer*", "");
            var frame = new Frame(new Window(facade), new Window(minibuffer));

            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            var modeLine = snapshot.windowSnapshots().get(0).modeLine();
            assertTrue(modeLine.contains("**"), "変更済みバッファは**を含む");
        }
    }

    @Nested
    class カーソル位置 {

        @Test
        void カーソルがバッファ先頭のとき画面左上に配置される() {
            var frame = createFrame("hello");
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            assertEquals(new CursorPosition(0, 0), snapshot.cursorPosition());
        }

        @Test
        void カーソルが2行目にあるとき画面の2行目に配置される() {
            var mainBuffer = createBuffer("main", "line1\nline2");
            var window = new Window(mainBuffer);
            // カーソルを2行目の先頭に移動（"line1\n"の長さ = 6コードポイント）
            window.setPoint(6);
            var minibuffer = createBuffer("*Minibuffer*", "");
            var frame = new Frame(window, new Window(minibuffer));

            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            assertEquals(new CursorPosition(0, 1), snapshot.cursorPosition());
        }

        @Test
        void カーソルが行の途中にあるときカラム位置が反映される() {
            var mainBuffer = createBuffer("main", "hello");
            var window = new Window(mainBuffer);
            window.setPoint(3); // "hel|lo"
            var minibuffer = createBuffer("*Minibuffer*", "");
            var frame = new Frame(window, new Window(minibuffer));

            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            assertEquals(new CursorPosition(3, 0), snapshot.cursorPosition());
        }
    }

    @Nested
    class ミニバッファ {

        @Test
        void ミニバッファ非アクティブでメッセージなしのとき空テキスト() {
            var frame = createFrame("hello");
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            assertFalse(snapshot.minibuffer().text().isPresent());
        }

        @Test
        void メッセージ表示中のときメッセージテキストが含まれる() {
            var frame = createFrame("hello");
            var msgBuffer = createMessageBuffer();
            msgBuffer.message("test message");

            var snapshot = RenderSnapshotFactory.create(frame, msgBuffer, 80, 24);

            assertTrue(snapshot.minibuffer().text().isPresent());
            assertEquals("test message", snapshot.minibuffer().text().get());
        }

        @Test
        void ミニバッファアクティブのときミニバッファ内容が含まれる() {
            var mainBuffer = createBuffer("main", "hello");
            var minibufferBuffer = createBuffer("*Minibuffer*", "");
            var minibufferWindow = new Window(minibufferBuffer);
            var frame = new Frame(new Window(mainBuffer), minibufferWindow);
            frame.activateMinibuffer();
            minibufferWindow.insert("find-file: ");

            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            assertTrue(snapshot.minibuffer().text().isPresent());
            assertEquals("find-file: ", snapshot.minibuffer().text().get());
        }
    }

    @Nested
    class スクロール位置調整 {

        @Test
        void カーソルが画面外にあるときdisplayStartLineが調整される() {
            var mainBuffer = createBuffer("main", "a\nb\nc\nd\ne\nf\ng");
            var window = new Window(mainBuffer);
            // カーソルを最終行に移動
            window.setPoint(mainBuffer.length());
            var minibuffer = createBuffer("*Minibuffer*", "");
            var frame = new Frame(window, new Window(minibuffer));

            // rows=4 → ウィンドウ領域3行 → バッファ表示2行
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 4);

            // 最終行(g)が表示されている
            var ws = snapshot.windowSnapshots().get(0);
            var lastLine = ws.visibleLines().get(ws.visibleLines().size() - 1);
            assertEquals("g", lastLine.text());
        }
    }

    @Nested
    class スタイリング {

        @Test
        void スタイラーのないモードでは行にスパン情報が付与されない() {
            var frame = createFrame("hello\nworld");
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            var ws = snapshot.windowSnapshots().get(0);
            for (var line : ws.visibleLines()) {
                assertFalse(line.spans().isPresent(), "TextModeにはスタイラーがないためスパンは空");
            }
        }
    }
}
