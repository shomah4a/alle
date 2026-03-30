package io.github.shomah4a.alle.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.styling.FaceName;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Direction;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Rect;
import io.github.shomah4a.alle.core.window.Window;
import io.github.shomah4a.alle.core.window.WindowLayout;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RenderSnapshotFactoryTest {

    private BufferFacade createBuffer(String name, String content) {
        var textModel = new GapTextModel();
        var buffer = new TextBuffer(name, textModel, new SettingsRegistry());
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
        return new MessageBuffer("*Messages*", 100, new SettingsRegistry());
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
            assertTrue(modeLine.contains("text"), "モード名を含む");
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
    class アクティブカーソル位置決定 {

        @Test
        void ミニバッファアクティブでアクティブウィンドウがミニバッファのときミニバッファ行にカーソルを置く() {
            var mainBuffer = createBuffer("main", "hello");
            var minibufferBuffer = createBuffer("*Minibuffer*", "find: abc");
            var mainWindow = new Window(mainBuffer);
            var minibufferWindow = new Window(minibufferBuffer);
            var frame = new Frame(mainWindow, minibufferWindow);
            frame.activateMinibuffer();
            minibufferWindow.setPoint(9); // "find: abc" の末尾

            int cols = 80;
            int rows = 24;
            int minibufferRow = rows - 1;
            var treeArea = new Rect(0, 0, cols, rows - 1);
            var layoutResult = WindowLayout.compute(frame.getWindowTree(), treeArea);

            var cursor = RenderSnapshotFactory.computeActiveCursorPosition(frame, layoutResult, minibufferRow, cols);

            assertEquals(new CursorPosition(9, 23), cursor);
        }

        @Test
        void ミニバッファアクティブでアクティブウィンドウがツリー内ウィンドウのときそのウィンドウにカーソルを置く() {
            var mainBuffer = createBuffer("main", "hello");
            var minibufferBuffer = createBuffer("*Minibuffer*", "find: ");
            var mainWindow = new Window(mainBuffer);
            mainWindow.setPoint(3); // "hel|lo"
            var minibufferWindow = new Window(minibufferBuffer);
            var frame = new Frame(mainWindow, minibufferWindow);
            frame.activateMinibuffer();
            frame.setActiveWindow(mainWindow); // other-windowでツリー内に移動した状態

            int cols = 80;
            int rows = 24;
            int minibufferRow = rows - 1;
            var treeArea = new Rect(0, 0, cols, rows - 1);
            var layoutResult = WindowLayout.compute(frame.getWindowTree(), treeArea);

            var cursor = RenderSnapshotFactory.computeActiveCursorPosition(frame, layoutResult, minibufferRow, cols);

            assertEquals(new CursorPosition(3, 0), cursor);
        }

        @Test
        void ミニバッファ非アクティブのときアクティブウィンドウにカーソルを置く() {
            var mainBuffer = createBuffer("main", "hello\nworld");
            var mainWindow = new Window(mainBuffer);
            mainWindow.setPoint(6); // "world"の先頭
            var minibufferBuffer = createBuffer("*Minibuffer*", "");
            var frame = new Frame(mainWindow, new Window(minibufferBuffer));

            int cols = 80;
            int rows = 24;
            int minibufferRow = rows - 1;
            var treeArea = new Rect(0, 0, cols, rows - 1);
            var layoutResult = WindowLayout.compute(frame.getWindowTree(), treeArea);

            var cursor = RenderSnapshotFactory.computeActiveCursorPosition(frame, layoutResult, minibufferRow, cols);

            assertEquals(new CursorPosition(0, 1), cursor);
        }

        @Test
        void ウィンドウ分割時にミニバッファからother_windowで移動した先にカーソルを置く() {
            var mainBuffer = createBuffer("main", "hello");
            var subBuffer = createBuffer("sub", "world");
            var mainWindow = new Window(mainBuffer);
            var minibufferBuffer = createBuffer("*Minibuffer*", "find: ");
            var minibufferWindow = new Window(minibufferBuffer);
            var frame = new Frame(mainWindow, minibufferWindow);
            var subWindow = frame.splitActiveWindow(Direction.VERTICAL, subBuffer);
            frame.activateMinibuffer();
            frame.setActiveWindow(subWindow); // other-windowでsubWindowに移動

            int cols = 80;
            int rows = 24;
            int minibufferRow = rows - 1;
            var treeArea = new Rect(0, 0, cols, rows - 1);
            var layoutResult = WindowLayout.compute(frame.getWindowTree(), treeArea);

            var cursor = RenderSnapshotFactory.computeActiveCursorPosition(frame, layoutResult, minibufferRow, cols);

            // subWindowはツリー内ウィンドウなのでミニバッファ行ではない
            var subRect = layoutResult.windowRects().get(subWindow);
            assertEquals(subRect.top(), cursor.row());
            assertEquals(subRect.left(), cursor.column());
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
    class モードライン_マイナーモード表示 {

        @Test
        void マイナーモードが有効な場合モードラインにマイナーモード名が表示される() {
            var mainBuffer = createBuffer("main", "hello");
            mainBuffer.enableMinorMode(new MinorMode() {
                @Override
                public String name() {
                    return "TestMinor";
                }

                @Override
                public Optional<Keymap> keymap() {
                    return Optional.empty();
                }
            });
            var minibuffer = createBuffer("*Minibuffer*", "");
            var frame = new Frame(new Window(mainBuffer), new Window(minibuffer));

            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);
            var modeLine = snapshot.windowSnapshots().get(0).modeLine();
            assertTrue(modeLine.contains("TestMinor"), "マイナーモード名を含む");
        }

        @Test
        void マイナーモードが無効な場合モードラインにマイナーモード名が表示されない() {
            var frame = createFrame("hello");
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);
            var modeLine = snapshot.windowSnapshots().get(0).modeLine();
            assertFalse(modeLine.contains("TestMinor"), "マイナーモード名を含まない");
            assertTrue(modeLine.contains("(text)"), "メジャーモード名のみ表示される");
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

    @Nested
    class リージョン {

        @Test
        void マーク設定時にリージョン範囲がスナップショットに含まれる() {
            var mainBuffer = createBuffer("main", "hello world");
            var window = new Window(mainBuffer);
            window.setMark(0);
            window.setPoint(5);
            var minibuffer = createBuffer("*Minibuffer*", "");
            var frame = new Frame(window, new Window(minibuffer));

            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            var ws = snapshot.windowSnapshots().get(0);
            assertTrue(ws.regionRange().isPresent());
            assertEquals(0, ws.regionRange().get().start());
            assertEquals(5, ws.regionRange().get().end());
        }

        @Test
        void マーク未設定時にリージョン範囲がemptyになる() {
            var frame = createFrame("hello world");

            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            var ws = snapshot.windowSnapshots().get(0);
            assertFalse(ws.regionRange().isPresent());
        }

        @Test
        void 行内リージョンが行ローカルのコードポイントオフセットで計算される() {
            var mainBuffer = createBuffer("main", "hello world");
            var window = new Window(mainBuffer);
            window.setMark(2);
            window.setPoint(8);
            var minibuffer = createBuffer("*Minibuffer*", "");
            var frame = new Frame(window, new Window(minibuffer));

            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            var ws = snapshot.windowSnapshots().get(0);
            var lineRegion = ws.visibleLines().get(0).regionInLine();
            assertTrue(lineRegion.isPresent());
            assertEquals(2, lineRegion.get().startCp());
            assertEquals(8, lineRegion.get().endCp());
        }

        @Test
        void 複数行にまたがるリージョンで各行の範囲が正しく計算される() {
            var mainBuffer = createBuffer("main", "aaa\nbbb\nccc");
            var window = new Window(mainBuffer);
            window.setMark(1); // "aaa" の2文字目
            window.setPoint(9); // "ccc" の2文字目
            var minibuffer = createBuffer("*Minibuffer*", "");
            var frame = new Frame(window, new Window(minibuffer));

            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            var ws = snapshot.windowSnapshots().get(0);
            // 1行目: offset 0-3 ("aaa"), region 1-3 → lineLocal 1-3
            var r0 = ws.visibleLines().get(0).regionInLine();
            assertTrue(r0.isPresent());
            assertEquals(1, r0.get().startCp());
            assertEquals(3, r0.get().endCp());

            // 2行目: offset 4-7 ("bbb"), region covers entire line → lineLocal 0-3
            var r1 = ws.visibleLines().get(1).regionInLine();
            assertTrue(r1.isPresent());
            assertEquals(0, r1.get().startCp());
            assertEquals(3, r1.get().endCp());

            // 3行目: offset 8-10 ("ccc"), region 8-9 → lineLocal 0-1
            var r2 = ws.visibleLines().get(2).regionInLine();
            assertTrue(r2.isPresent());
            assertEquals(0, r2.get().startCp());
            assertEquals(1, r2.get().endCp());
        }

        @Test
        void リージョン範囲外の行にはlineRegionがemptyになる() {
            var mainBuffer = createBuffer("main", "aaa\nbbb\nccc");
            var window = new Window(mainBuffer);
            window.setMark(4); // "bbb" の先頭
            window.setPoint(7); // "bbb" の末尾
            var minibuffer = createBuffer("*Minibuffer*", "");
            var frame = new Frame(window, new Window(minibuffer));

            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            var ws = snapshot.windowSnapshots().get(0);
            assertFalse(ws.visibleLines().get(0).regionInLine().isPresent());
            assertTrue(ws.visibleLines().get(1).regionInLine().isPresent());
            assertFalse(ws.visibleLines().get(2).regionInLine().isPresent());
        }
    }

    @Nested
    class テキストプロパティface {

        @Test
        void スタイラーなしのバッファにfaceを設定するとLineSnapshotにスパンが付与される() {
            var mainBuffer = createBuffer("main", "hello world");
            mainBuffer.putFace(0, 5, FaceName.STRONG);
            var minibuffer = createBuffer("*Minibuffer*", "");
            var frame = new Frame(new Window(mainBuffer), new Window(minibuffer));

            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            var ws = snapshot.windowSnapshots().get(0);
            var line = ws.visibleLines().get(0);
            assertTrue(line.spans().isPresent());
            var spans = line.spans().get();
            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(5, spans.get(0).end());
            assertEquals(FaceName.STRONG, spans.get(0).faceName());
        }

        @Test
        void face未設定のバッファではスタイラーなし時にスパンが付与されない() {
            var frame = createFrame("hello world");

            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            var ws = snapshot.windowSnapshots().get(0);
            var line = ws.visibleLines().get(0);
            assertFalse(line.spans().isPresent());
        }

        @Test
        void 別の行にfaceを設定すると行ローカル座標に変換される() {
            var mainBuffer = createBuffer("main", "hello\nworld");
            // "world"はバッファオフセット6-11だが、行ローカルでは0-5
            mainBuffer.putFace(6, 11, FaceName.KEYWORD);
            var minibuffer = createBuffer("*Minibuffer*", "");
            var frame = new Frame(new Window(mainBuffer), new Window(minibuffer));

            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            var ws = snapshot.windowSnapshots().get(0);
            // 1行目にはスパンなし
            assertFalse(ws.visibleLines().get(0).spans().isPresent());
            // 2行目にfaceスパンが行ローカル座標で付与される
            var line2 = ws.visibleLines().get(1);
            assertTrue(line2.spans().isPresent());
            var spans = line2.spans().get();
            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(5, spans.get(0).end());
            assertEquals(FaceName.KEYWORD, spans.get(0).faceName());
        }

        @Test
        void ミニバッファにfaceを設定するとMinibufferSnapshotにスパンが付与される() {
            var mainBuffer = createBuffer("main", "hello");
            var minibufferBuffer = createBuffer("*Minibuffer*", "");
            var minibufferWindow = new Window(minibufferBuffer);
            var frame = new Frame(new Window(mainBuffer), minibufferWindow);
            frame.activateMinibuffer();
            minibufferWindow.insert("Find file: /path");
            minibufferBuffer.putFace(0, 11, FaceName.PROMPT);

            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            var mb = snapshot.minibuffer();
            assertTrue(mb.spans().isPresent());
            var spans = mb.spans().get();
            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(11, spans.get(0).end());
            assertEquals(FaceName.PROMPT, spans.get(0).faceName());
        }

        @Test
        void ミニバッファにface未設定のときMinibufferSnapshotにスパンが付与されない() {
            var mainBuffer = createBuffer("main", "hello");
            var minibufferBuffer = createBuffer("*Minibuffer*", "");
            var minibufferWindow = new Window(minibufferBuffer);
            var frame = new Frame(new Window(mainBuffer), minibufferWindow);
            frame.activateMinibuffer();
            minibufferWindow.insert("Find file: /path");

            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 80, 24);

            assertFalse(snapshot.minibuffer().spans().isPresent());
        }
    }

    @Nested
    class 折り返しモード {

        @Test
        void 折り返しモード時に1バッファ行が複数の視覚行に展開される() {
            // 幅5のウィンドウで10文字の行 → 2視覚行
            var frame = createFrame("abcdefghij");
            frame.getActiveWindow().setPoint(0);
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 5, 5);
            // モードライン1行を除く4行分の表示領域で、2視覚行が生成される
            var ws = snapshot.windowSnapshots().get(0);
            assertEquals(2, ws.visibleLines().size());
            // 各視覚行にvisualLineRangeが設定されている
            assertTrue(ws.visibleLines().get(0).visualLineRange().isPresent());
            assertTrue(ws.visibleLines().get(1).visualLineRange().isPresent());
            // 1行目は cp0-5, 2行目は cp5-10
            var vl0 = ws.visibleLines().get(0).visualLineRange().get();
            assertEquals(0, vl0.startCp());
            assertEquals(5, vl0.endCp());
            var vl1 = ws.visibleLines().get(1).visualLineRange().get();
            assertEquals(5, vl1.startCp());
            assertEquals(10, vl1.endCp());
        }

        @Test
        void 長大行でカーソルが後方の視覚行にある場合もカーソルが画面内に収まる() {
            // 幅5で15文字の1行 → 3視覚行（cp0-5, cp5-10, cp10-15）
            // 表示3行必要: height = 3(buffer) + 1(modeline) + 1(minibuf) = 5
            var frame = createFrame("abcdefghijklmno");
            frame.getActiveWindow().setPoint(15); // 末尾
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 5, 5);
            var ws = snapshot.windowSnapshots().get(0);
            assertEquals(3, ws.visibleLines().size());
            var cursor = snapshot.cursorPosition();
            assertTrue(cursor.row() >= 0 && cursor.row() < 3);
        }

        @Test
        void 画面行数を超える長大行でdisplayStartVisualLineが設定される() {
            // 幅5で15文字の1行 → 3視覚行だが表示2行しかない
            // height = 2(buffer) + 1(modeline) + 1(minibuf) = 4
            // カーソルを末尾に置くと、displayStartVisualLine=1で視覚行1-2が表示される
            var frame = createFrame("abcdefghijklmno");
            frame.getActiveWindow().setPoint(15); // 末尾
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 5, 4);
            var ws = snapshot.windowSnapshots().get(0);
            assertEquals(2, ws.visibleLines().size());
            // 先頭の視覚行がスキップされて、視覚行1から表示される
            var vl0 = ws.visibleLines().get(0).visualLineRange().get();
            assertEquals(5, vl0.startCp()); // 視覚行1の開始
            var vl1 = ws.visibleLines().get(1).visualLineRange().get();
            assertEquals(10, vl1.startCp()); // 視覚行2の開始
            // カーソルが画面内にある
            var cursor = snapshot.cursorPosition();
            assertTrue(cursor.row() >= 0 && cursor.row() < 2);
        }

        @Test
        void 画面行数を超える長大行でカーソルが先頭にある場合は視覚行0から表示される() {
            // 幅5で15文字の1行、表示2行
            var frame = createFrame("abcdefghijklmno");
            frame.getActiveWindow().setPoint(0);
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 5, 4);
            var ws = snapshot.windowSnapshots().get(0);
            assertEquals(2, ws.visibleLines().size());
            var vl0 = ws.visibleLines().get(0).visualLineRange().get();
            assertEquals(0, vl0.startCp());
            var cursor = snapshot.cursorPosition();
            assertEquals(0, cursor.column());
            assertEquals(0, cursor.row());
        }

        @Test
        void 画面行数を超える長大行でカーソルが中央にある場合() {
            // 幅5で25文字の1行 → 5視覚行、表示2行
            // height = 2(buffer) + 1(modeline) + 1(minibuf) = 4
            // カーソルをcp12に置く → 視覚行2
            var frame = createFrame("abcdefghijklmnopqrstuvwxy");
            frame.getActiveWindow().setPoint(12);
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 5, 4);
            var ws = snapshot.windowSnapshots().get(0);
            assertEquals(2, ws.visibleLines().size());
            var cursor = snapshot.cursorPosition();
            assertTrue(cursor.row() >= 0 && cursor.row() < 2);
        }

        @Test
        void 切り詰めモード時はvisualLineRangeが設定されない() {
            var frame = createFrame("abcdefghij");
            frame.getActiveWindow().setTruncateLines(true);
            frame.getActiveWindow().setPoint(0);
            var snapshot = RenderSnapshotFactory.create(frame, createMessageBuffer(), 5, 5);
            var ws = snapshot.windowSnapshots().get(0);
            for (var line : ws.visibleLines()) {
                assertTrue(line.visualLineRange().isEmpty());
            }
        }
    }
}
