package io.github.shomah4a.alle.core.window;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WindowTest {

    private BufferFacade createBuffer() {
        return new BufferFacade(new TextBuffer("test", new GapTextModel(), new SettingsRegistry()));
    }

    private Window createWindow() {
        return new Window(createBuffer());
    }

    @Nested
    class 初期状態 {

        @Test
        void バッファを保持しポイントは0() {
            var window = createWindow();
            assertEquals("test", window.getBuffer().getName());
            assertEquals(0, window.getPoint());
            assertEquals(0, window.getDisplayStartLine());
        }
    }

    @Nested
    class カーソル位置での挿入 {

        @Test
        void カーソル位置に文字列を挿入してカーソルが後ろに移動する() {
            var window = createWindow();
            window.insert("Hello");
            assertEquals("Hello", window.getBuffer().getText());
            assertEquals(5, window.getPoint());
            assertTrue(window.getBuffer().isDirty());
        }

        @Test
        void 連続挿入でカーソル位置が進む() {
            var window = createWindow();
            window.insert("Hello");
            window.insert(" World");
            assertEquals("Hello World", window.getBuffer().getText());
            assertEquals(11, window.getPoint());
        }

        @Test
        void 絵文字を挿入してもカーソル位置がコードポイント単位で正しい() {
            var window = createWindow();
            window.insert("A😀B");
            assertEquals(3, window.getPoint());
        }
    }

    @Nested
    class バックスペースとデリート {

        @Test
        void バックスペースでカーソル手前の文字を削除する() {
            var window = createWindow();
            window.insert("Hello");
            window.deleteBackward(1);
            assertEquals("Hell", window.getBuffer().getText());
            assertEquals(4, window.getPoint());
        }

        @Test
        void 先頭でバックスペースしても何も起きない() {
            var window = createWindow();
            window.insert("Hello");
            window.setPoint(0);
            window.deleteBackward(1);
            assertEquals("Hello", window.getBuffer().getText());
            assertEquals(0, window.getPoint());
        }

        @Test
        void デリートでカーソル後ろの文字を削除する() {
            var window = createWindow();
            window.insert("Hello");
            window.setPoint(0);
            window.deleteForward(1);
            assertEquals("ello", window.getBuffer().getText());
            assertEquals(0, window.getPoint());
        }

        @Test
        void 末尾でデリートしても何も起きない() {
            var window = createWindow();
            window.insert("Hello");
            window.deleteForward(1);
            assertEquals("Hello", window.getBuffer().getText());
            assertEquals(5, window.getPoint());
        }
    }

    @Nested
    class ポイント設定 {

        @Test
        void 範囲内のポイントを設定できる() {
            var window = createWindow();
            window.insert("Hello");
            window.setPoint(3);
            assertEquals(3, window.getPoint());
        }

        @Test
        void 末尾位置にポイントを設定できる() {
            var window = createWindow();
            window.insert("Hello");
            window.setPoint(5);
            assertEquals(5, window.getPoint());
        }

        @Test
        void 範囲外のポイント設定で例外が発生する() {
            var window = createWindow();
            window.insert("Hello");
            assertThrows(IndexOutOfBoundsException.class, () -> window.setPoint(6));
            assertThrows(IndexOutOfBoundsException.class, () -> window.setPoint(-1));
        }
    }

    @Nested
    class 表示開始行 {

        @Test
        void 表示開始行を設定できる() {
            var window = createWindow();
            window.insert("Hello\nWorld\nFoo");
            window.setDisplayStartLine(1);
            assertEquals(1, window.getDisplayStartLine());
        }

        @Test
        void 範囲外の行で例外が発生する() {
            var window = createWindow();
            window.insert("Hello\nWorld");
            assertThrows(IndexOutOfBoundsException.class, () -> window.setDisplayStartLine(2));
            assertThrows(IndexOutOfBoundsException.class, () -> window.setDisplayStartLine(-1));
        }
    }

    @Nested
    class スクロール調整 {

        @Test
        void カーソルが表示範囲より下にある場合displayStartLineが進む() {
            var window = createWindow();
            window.insert("line1\nline2\nline3\nline4\nline5");
            // カーソルは最終行(line5)にいる
            // 表示可能行数3でensurePointVisibleを呼ぶ
            window.ensurePointVisible(3);
            // line5(index=4)が見えるように、displayStartLine = 4 - 3 + 1 = 2
            assertEquals(2, window.getDisplayStartLine());
        }

        @Test
        void カーソルが表示範囲より上にある場合displayStartLineが戻る() {
            var window = createWindow();
            window.insert("line1\nline2\nline3\nline4\nline5");
            window.setDisplayStartLine(3);
            // カーソルを先頭行に移動
            window.setPoint(0);
            window.ensurePointVisible(3);
            assertEquals(0, window.getDisplayStartLine());
        }

        @Test
        void カーソルが表示範囲内の場合displayStartLineは変わらない() {
            var window = createWindow();
            window.insert("line1\nline2\nline3\nline4\nline5");
            window.setDisplayStartLine(1);
            // カーソルをline3(index=2)に移動 — 表示範囲[1,3]に含まれる
            window.setPoint(12); // "line1\nline2\n" = 12
            window.ensurePointVisible(3);
            assertEquals(1, window.getDisplayStartLine());
        }

        @Test
        void visibleRowsが0以下の場合何もしない() {
            var window = createWindow();
            window.insert("line1\nline2");
            window.ensurePointVisible(0);
            assertEquals(0, window.getDisplayStartLine());
        }
    }

    @Nested
    class 水平スクロール調整 {

        @Test
        void カーソルが表示範囲より右にある場合displayStartColumnが進む() {
            var window = createWindow();
            window.setTruncateLines(true);
            // "abcdefghij" = 10文字、visibleColumns=5
            window.insert("abcdefghij");
            // カーソルはcol=10
            window.ensurePointHorizontallyVisible(5);
            // cursorColumn(10) >= displayStartColumn(0) + 5 → newStart = 10 - 5 + 1 = 6
            assertEquals(6, window.getDisplayStartColumn());
        }

        @Test
        void カーソルが表示範囲より左にある場合displayStartColumnが戻る() {
            var window = createWindow();
            window.setTruncateLines(true);
            window.insert("abcdefghij");
            window.ensurePointHorizontallyVisible(5);
            // displayStartColumn = 6
            window.setPoint(2); // col=2
            window.ensurePointHorizontallyVisible(5);
            assertEquals(2, window.getDisplayStartColumn());
        }

        @Test
        void カーソルが表示範囲内の場合displayStartColumnは変わらない() {
            var window = createWindow();
            window.setTruncateLines(true);
            window.insert("abcdefghij");
            window.ensurePointHorizontallyVisible(5);
            // displayStartColumn = 6, cursor at col=10
            window.setPoint(8); // col=8, 6 <= 8 < 11 → 範囲内
            window.ensurePointHorizontallyVisible(5);
            assertEquals(6, window.getDisplayStartColumn());
        }

        @Test
        void 全角文字でカーソルが右に出る場合displayStartColumnが丸められる() {
            var window = createWindow();
            window.setTruncateLines(true);
            // "あいうえお" = 全角5文字、各2カラム、合計10カラム
            window.insert("あいうえお");
            // カーソルはcol=10、visibleColumns=6
            window.ensurePointHorizontallyVisible(6);
            // newStart = 10 - 6 + 1 = 5 → 全角文字途中 → snap → 4
            assertEquals(4, window.getDisplayStartColumn());
        }

        @Test
        void visibleColumnsが0以下の場合何もしない() {
            var window = createWindow();
            window.insert("abcde");
            window.ensurePointHorizontallyVisible(0);
            assertEquals(0, window.getDisplayStartColumn());
        }

        @Test
        void バッファ切り替えでdisplayStartColumnがリセットされる() {
            var window = createWindow();
            window.setTruncateLines(true);
            window.insert("abcdefghij");
            window.ensurePointHorizontallyVisible(5);
            assertTrue(window.getDisplayStartColumn() > 0);

            window.setBuffer(new BufferFacade(new TextBuffer("new", new GapTextModel(), new SettingsRegistry())));
            assertEquals(0, window.getDisplayStartColumn());
        }
    }

    @Nested
    class バッファ切り替え {

        @Test
        void バッファを切り替えるとポイントと表示開始行がリセットされる() {
            var window = createWindow();
            window.insert("Hello");
            window.setPoint(3);

            var newBuffer = new BufferFacade(new TextBuffer("other", new GapTextModel(), new SettingsRegistry()));
            window.setBuffer(newBuffer);

            assertEquals("other", window.getBuffer().getName());
            assertEquals(0, window.getPoint());
            assertEquals(0, window.getDisplayStartLine());
        }
    }

    @Nested
    class バッファ履歴 {

        @Test
        void 初期状態では履歴が空() {
            var window = createWindow();
            assertTrue(window.getBufferHistory().isEmpty());
        }

        @Test
        void バッファ切り替え後に旧バッファ名が履歴先頭に記録される() {
            var window = createWindow();
            var newBuffer = new BufferFacade(new TextBuffer("new", new GapTextModel(), new SettingsRegistry()));

            window.setBuffer(newBuffer);

            assertEquals(1, window.getBufferHistory().size());
            assertEquals(
                    new BufferHistoryEntry.ByName("test"),
                    window.getBufferHistory().get(0));
        }

        @Test
        void 指定エントリを履歴から除去できる() {
            var window = createWindow();
            var newBuffer = new BufferFacade(new TextBuffer("new", new GapTextModel(), new SettingsRegistry()));
            window.setBuffer(newBuffer);

            window.removeFromBufferHistory(new BufferHistoryEntry.ByName("test"));

            assertTrue(window.getBufferHistory().isEmpty());
        }

        @Test
        void 一致しないエントリの除去では履歴が変わらない() {
            var window = createWindow();
            var newBuffer = new BufferFacade(new TextBuffer("new", new GapTextModel(), new SettingsRegistry()));
            window.setBuffer(newBuffer);

            window.removeFromBufferHistory(new BufferHistoryEntry.ByName("unrelated"));

            assertEquals(1, window.getBufferHistory().size());
            assertEquals(
                    new BufferHistoryEntry.ByName("test"),
                    window.getBufferHistory().get(0));
        }

        @Test
        void 複数回切り替えると履歴がMRU順に記録される() {
            var window = createWindow();
            var bufferB = new BufferFacade(new TextBuffer("b", new GapTextModel(), new SettingsRegistry()));
            var bufferC = new BufferFacade(new TextBuffer("c", new GapTextModel(), new SettingsRegistry()));

            window.setBuffer(bufferB);
            window.setBuffer(bufferC);

            assertEquals(2, window.getBufferHistory().size());
            assertEquals(
                    new BufferHistoryEntry.ByName("b"),
                    window.getBufferHistory().get(0));
            assertEquals(
                    new BufferHistoryEntry.ByName("test"),
                    window.getBufferHistory().get(1));
        }

        @Test
        void 既に履歴にあるバッファに切り替えると先頭に移動する() {
            var window = createWindow();
            var bufferB = new BufferFacade(new TextBuffer("b", new GapTextModel(), new SettingsRegistry()));
            var bufferC = new BufferFacade(new TextBuffer("c", new GapTextModel(), new SettingsRegistry()));
            var originalBuffer = window.getBuffer();

            window.setBuffer(bufferB);
            window.setBuffer(bufferC);
            window.setBuffer(originalBuffer); // testに戻る

            assertEquals(3, window.getBufferHistory().size());
            assertEquals(
                    new BufferHistoryEntry.ByName("c"),
                    window.getBufferHistory().get(0));
            assertEquals(
                    new BufferHistoryEntry.ByName("b"),
                    window.getBufferHistory().get(1));
            assertEquals(
                    new BufferHistoryEntry.ByName("test"),
                    window.getBufferHistory().get(2));
        }

        @Test
        void 同一バッファへの切り替えは履歴に記録されない() {
            var window = createWindow();
            var currentBuffer = window.getBuffer();

            window.setBuffer(currentBuffer);

            assertTrue(window.getBufferHistory().isEmpty());
        }

        @Test
        void ファイルパスを持つバッファはByPathで記録される() {
            var buffer = new BufferFacade(new TextBuffer("file.txt", new GapTextModel(), new SettingsRegistry()));
            var filePath = java.nio.file.Path.of("/tmp/file.txt");
            buffer.setFilePath(filePath);
            var window = new Window(buffer);
            var newBuffer = new BufferFacade(new TextBuffer("other", new GapTextModel(), new SettingsRegistry()));

            window.setBuffer(newBuffer);

            assertEquals(1, window.getBufferHistory().size());
            assertEquals(
                    new BufferHistoryEntry.ByPath(filePath),
                    window.getBufferHistory().get(0));
        }
    }

    @Nested
    class マーク {

        @Test
        void 初期状態ではmarkが未設定() {
            var window = createWindow();
            assertTrue(window.getMark().isEmpty());
        }

        @Test
        void markを設定して取得できる() {
            var window = createWindow();
            window.insert("Hello");
            window.setMark(2);
            assertEquals(2, window.getMark().orElseThrow());
        }

        @Test
        void markをクリアするとemptyになる() {
            var window = createWindow();
            window.insert("Hello");
            window.setMark(2);
            window.clearMark();
            assertTrue(window.getMark().isEmpty());
        }

        @Test
        void 範囲外のmark設定で例外が発生する() {
            var window = createWindow();
            window.insert("Hello");
            assertThrows(IndexOutOfBoundsException.class, () -> window.setMark(6));
            assertThrows(IndexOutOfBoundsException.class, () -> window.setMark(-1));
        }

        @Test
        void バッファ切り替えでmarkがクリアされる() {
            var window = createWindow();
            window.insert("Hello");
            window.setMark(2);
            window.setBuffer(new BufferFacade(new TextBuffer("other", new GapTextModel(), new SettingsRegistry())));
            assertTrue(window.getMark().isEmpty());
        }

        @Test
        void markがバッファ長を超過した場合clampされる() {
            var buffer = createBuffer();
            var window1 = new Window(buffer);
            var window2 = new Window(buffer);
            window1.insert("Hello");
            window2.setMark(5);
            window1.setPoint(0);
            window1.deleteForward(3);
            // バッファ長が2なので、markは2にclampされる
            assertEquals(2, window2.getMark().orElseThrow());
        }
    }

    @Nested
    class リージョン {

        @Test
        void markが未設定の場合regionStartはempty() {
            var window = createWindow();
            window.insert("Hello");
            assertTrue(window.getRegionStart().isEmpty());
            assertTrue(window.getRegionEnd().isEmpty());
        }

        @Test
        void markがpointより前にある場合() {
            var window = createWindow();
            window.insert("Hello");
            window.setMark(1);
            window.setPoint(4);
            assertEquals(1, window.getRegionStart().orElseThrow());
            assertEquals(4, window.getRegionEnd().orElseThrow());
        }

        @Test
        void markがpointより後にある場合() {
            var window = createWindow();
            window.insert("Hello");
            window.setMark(4);
            window.setPoint(1);
            assertEquals(1, window.getRegionStart().orElseThrow());
            assertEquals(4, window.getRegionEnd().orElseThrow());
        }
    }

    @Nested
    class pointGuardによるカーソル進入禁止 {

        @Test
        void 後方移動でガード範囲に入ると手前に押し戻される() {
            // ガード[3,8)、point=10からsetPoint(5) → 後方移動 → start-1=2に押し戻す
            var window = createWindow();
            window.insert("AB Prompt Hello");
            window.getBuffer().putPointGuard(3, 10);
            window.setPoint(12);
            window.setPoint(5);
            assertEquals(2, window.getPoint());
        }

        @Test
        void 先頭から始まるガード範囲への後方移動はend位置に押し出される() {
            // ガード[0,8)、後方移動で入るとstart-1が負 → end=8に押し出す
            var window = createWindow();
            window.insert("Prompt: Hello");
            window.getBuffer().putPointGuard(0, 8);
            window.setPoint(10);
            window.setPoint(5);
            assertEquals(8, window.getPoint());
        }

        @Test
        void 前方移動でガード範囲に入るとend位置に押し出される() {
            var window = createWindow();
            window.insert("Prompt: Hello");
            window.getBuffer().putPointGuard(0, 8);
            // point=0 → setPoint(5)で前方移動 → ガード[0,8)内 → end=8に押し出す
            window.setPoint(5);
            assertEquals(8, window.getPoint());
        }

        @Test
        void ガード範囲外への移動は影響を受けない() {
            var window = createWindow();
            window.insert("Prompt: Hello");
            window.getBuffer().putPointGuard(0, 8);
            window.setPoint(10);
            assertEquals(10, window.getPoint());
        }

        @Test
        void ガードのend位置にカーソルを設定できる() {
            var window = createWindow();
            window.insert("Prompt: Hello");
            window.getBuffer().putPointGuard(0, 8);
            window.setPoint(8);
            assertEquals(8, window.getPoint());
        }

        @Test
        void getPointでガード範囲内のpointがend位置にクランプされる() {
            var window = createWindow();
            window.insert("Prompt: Hello");
            // pointを5に設定してからガードを設定
            window.setPoint(5);
            window.getBuffer().putPointGuard(0, 8);
            // getPointがガード範囲外に押し出す
            assertEquals(8, window.getPoint());
        }

        @Test
        void deleteBackwardでガード範囲内の文字は削除されない() {
            var window = createWindow();
            window.insert("Prompt: Hello");
            window.getBuffer().putPointGuard(0, 8);
            window.setPoint(10);
            // 10文字削除しようとしても、ガード境界(8)までの2文字しか削除されない
            window.deleteBackward(10);
            assertEquals("Prompt: llo", window.getBuffer().getText());
            assertEquals(8, window.getPoint());
        }

        @Test
        void deleteForwardでガード範囲内の文字は削除されない() {
            var window = createWindow();
            window.insert("AB GUARD CD");
            // ガードをread-onlyなしで設定（pointGuard単独で動作を確認）
            window.getBuffer().putPointGuard(3, 8);
            window.setPoint(1);
            // 10文字削除しようとしても、ガード開始位置(3)までの2文字("B ")しか削除されない
            // 削除後ガード範囲は[1,6)にシフトし、point=1はガード内のためend=6に押し出される
            window.deleteForward(10);
            assertEquals("AGUARD CD", window.getBuffer().getText());
            assertEquals(6, window.getPoint());
        }

        @Test
        void ガード解除後はカーソルが自由に移動できる() {
            var window = createWindow();
            window.insert("Prompt: Hello");
            window.getBuffer().putPointGuard(0, 8);
            window.getBuffer().removePointGuard(0, 8);
            window.setPoint(5);
            assertEquals(5, window.getPoint());
        }
    }

    @Nested
    class 行切り詰めモード {

        @Test
        void デフォルトは折り返しモード() {
            var window = createWindow();
            assertFalse(window.isTruncateLines());
        }

        @Test
        void 折り返しモードに切り替えるとdisplayStartColumnが0にリセットされる() {
            var window = createWindow();
            window.setTruncateLines(true);
            window.insert("abcdefghijklmnopqrstuvwxyz");
            window.ensurePointHorizontallyVisible(10);
            // 水平スクロール位置がリセットされる
            window.setTruncateLines(false);
            assertEquals(0, window.getDisplayStartColumn());
        }

        @Test
        void 折り返しモード時にensurePointHorizontallyVisibleがdisplayStartColumnを0に保つ() {
            var window = createWindow();
            window.insert("abcdefghijklmnopqrstuvwxyz");
            window.setTruncateLines(false);
            window.ensurePointHorizontallyVisible(10);
            assertEquals(0, window.getDisplayStartColumn());
        }

        @Test
        void 切り詰めモード時は従来通り水平スクロールが有効() {
            var window = createWindow();
            window.insert("abcdefghijklmnopqrstuvwxyz");
            window.setTruncateLines(true);
            window.ensurePointHorizontallyVisible(10);
            assertTrue(window.getDisplayStartColumn() > 0);
        }

        @Test
        void バッファ切り替え後もtruncateLinesが保持される() {
            var window = createWindow();
            window.setTruncateLines(false);
            assertFalse(window.isTruncateLines());
            window.setBuffer(new BufferFacade(new TextBuffer("new", new GapTextModel(), new SettingsRegistry())));
            assertFalse(window.isTruncateLines());
        }
    }

    @Nested
    class 同一バッファの複数ウィンドウ {

        @Test
        void 同じバッファを持つ複数ウィンドウが独立したポイントを持てる() {
            var buffer = createBuffer();
            var window1 = new Window(buffer);
            var window2 = new Window(buffer);

            window1.insert("Hello World");
            window1.setPoint(5);
            window2.setPoint(0);

            assertEquals(5, window1.getPoint());
            assertEquals(0, window2.getPoint());
        }

        @Test
        void 片方のウィンドウでの編集がもう片方のバッファにも反映される() {
            var buffer = createBuffer();
            var window1 = new Window(buffer);
            var window2 = new Window(buffer);

            window1.insert("Hello");
            assertEquals("Hello", window2.getBuffer().getText());
        }

        @Test
        void 片方のウィンドウでの削除でもう片方のポイントがバッファ長を超過した場合clampされる() {
            var buffer = createBuffer();
            var window1 = new Window(buffer);
            var window2 = new Window(buffer);

            window1.insert("Hello");
            window2.setPoint(5);
            // window1で全削除するとバッファ長が0になる
            window1.setPoint(0);
            window1.deleteForward(5);

            assertEquals(0, window2.getPoint());
        }
    }

    @Nested
    class ビュー状態のキャプチャと復元 {

        @Test
        void 現在のビュー状態をキャプチャできる() {
            var window = createWindow();
            window.insert("Hello\nWorld\nFoo");
            window.setPoint(7);
            window.setMark(3);

            var state = window.captureViewState();

            assertEquals(7, state.point());
            assertEquals(0, state.displayStartLine());
            assertEquals(0, state.displayStartVisualLine());
            assertEquals(0, state.displayStartColumn());
            assertEquals(3, state.mark());
        }

        @Test
        void ビュー状態を復元できる() {
            var window = createWindow();
            window.insert("Hello\nWorld\nFoo");
            var state = new ViewState(7, 1, 0, 0, 3);

            window.restoreViewState(state);

            assertEquals(7, window.getPoint());
            assertEquals(1, window.getDisplayStartLine());
            assertEquals(3, window.getMark().orElseThrow());
        }

        @Test
        void 復元時にpointがバッファ長を超過していた場合クランプされる() {
            var window = createWindow();
            window.insert("Hi");
            var state = new ViewState(100, 0, 0, 0, null);

            window.restoreViewState(state);

            assertEquals(2, window.getPoint());
        }

        @Test
        void 復元時にdisplayStartLineが行数を超過していた場合クランプされる() {
            var window = createWindow();
            window.insert("Hello\nWorld");
            // 2行のバッファに対してdisplayStartLine=10
            var state = new ViewState(0, 10, 0, 0, null);

            window.restoreViewState(state);

            assertEquals(1, window.getDisplayStartLine());
        }

        @Test
        void 復元時にmarkがバッファ長を超過していた場合クランプされる() {
            var window = createWindow();
            window.insert("Hi");
            var state = new ViewState(0, 0, 0, 0, 100);

            window.restoreViewState(state);

            assertEquals(2, window.getMark().orElseThrow());
        }

        @Test
        void markがnullの状態を復元できる() {
            var window = createWindow();
            window.insert("Hello");
            window.setMark(2);
            var state = new ViewState(0, 0, 0, 0, null);

            window.restoreViewState(state);

            assertTrue(window.getMark().isEmpty());
        }
    }
}
