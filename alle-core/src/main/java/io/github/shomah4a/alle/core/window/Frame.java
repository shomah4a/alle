package io.github.shomah4a.alle.core.window;

import io.github.shomah4a.alle.core.DisplayWidthUtil;
import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.styling.StylingState;
import io.github.shomah4a.alle.core.styling.SyntaxStyler;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;

/**
 * エディタのフレーム。
 * ターミナル(TUI)またはOSウィンドウ(GUI)1つに対応する。
 * ウィンドウツリーとミニバッファウィンドウを保持し、ウィンドウの分割・削除・切り替えを管理する。
 * また、描画用のスナップショット生成の責務を持つ。
 */
public class Frame {

    private WindowTree windowTree;
    private final Window minibufferWindow;
    private Window activeWindow;
    private boolean minibufferActive;

    public Frame(Window initialWindow, Window minibufferWindow) {
        this.windowTree = new WindowTree.Leaf(initialWindow);
        this.minibufferWindow = minibufferWindow;
        this.activeWindow = initialWindow;
    }

    /**
     * 現在のウィンドウツリーを返す。
     */
    public WindowTree getWindowTree() {
        return windowTree;
    }

    /**
     * ミニバッファウィンドウを返す。
     */
    public Window getMinibufferWindow() {
        return minibufferWindow;
    }

    /**
     * アクティブウィンドウを返す。
     */
    public Window getActiveWindow() {
        return activeWindow;
    }

    /**
     * アクティブウィンドウを設定する。
     * ウィンドウツリーに含まれるウィンドウ、
     * またはミニバッファが入力受付中の場合はミニバッファウィンドウを設定可能。
     *
     * @throws IllegalArgumentException ウィンドウが設定可能な対象でない場合
     */
    public void setActiveWindow(Window window) {
        if (window == minibufferWindow && minibufferActive) {
            this.activeWindow = window;
            return;
        }
        if (!windowTree.contains(window)) {
            throw new IllegalArgumentException("Window is not in the window tree");
        }
        this.activeWindow = window;
    }

    /**
     * ミニバッファが入力受付中かどうかを返す。
     */
    public boolean isMinibufferActive() {
        return minibufferActive;
    }

    /**
     * ミニバッファを入力受付状態にし、アクティブウィンドウをミニバッファに切り替える。
     * 呼び出し前のアクティブウィンドウを記憶しておくのは呼び出し側の責務。
     */
    public void activateMinibuffer() {
        this.minibufferActive = true;
        this.activeWindow = minibufferWindow;
    }

    /**
     * ミニバッファの入力受付状態を解除する。
     * アクティブウィンドウの復帰は呼び出し側が行う。
     */
    public void deactivateMinibuffer() {
        this.minibufferActive = false;
    }

    /**
     * アクティブウィンドウを指定方向に分割する。
     * 新しいウィンドウは指定バッファを表示し、アクティブウィンドウは新しいウィンドウに切り替わる。
     *
     * @param direction 分割方向
     * @param buffer    新しいウィンドウに表示するバッファ
     * @return 新しく作成されたウィンドウ
     */
    public Window splitActiveWindow(Direction direction, Buffer buffer) {
        var newWindow = new Window(buffer);
        var result = windowTree.split(activeWindow, direction, newWindow);
        if (result.isEmpty()) {
            throw new IllegalStateException("Active window not found in tree");
        }
        windowTree = result.get();
        activeWindow = newWindow;
        return newWindow;
    }

    /**
     * アクティブウィンドウを指定方向に分割し、同一バッファを表示する。
     * アクティブウィンドウは元のウィンドウに留まる。
     * ミニバッファアクティブ中は何もしない。
     */
    public void splitActiveWindowKeepFocus(Direction direction) {
        var originalWindow = activeWindow;
        var buffer = originalWindow.getBuffer();
        splitActiveWindow(direction, buffer);
        activeWindow = originalWindow;
    }

    /**
     * アクティブウィンドウを削除する。
     * 最後の1つのウィンドウは削除できない。ミニバッファアクティブ中は何もしない。
     *
     * @return 削除に成功した場合true
     */
    public boolean deleteActiveWindow() {
        return deleteWindow(activeWindow);
    }

    /**
     * 指定ウィンドウを削除する。
     * 最後の1つのウィンドウは削除できない。
     * 削除対象がアクティブウィンドウの場合、ツリー内の別のウィンドウをアクティブにする。
     *
     * @return 削除に成功した場合true
     */
    public boolean deleteWindow(Window target) {
        var result = windowTree.remove(target);
        if (result.isEmpty()) {
            return false;
        }
        windowTree = result.get();
        if (activeWindow == target) {
            activeWindow = findFirstWindow(windowTree);
        }
        return true;
    }

    /**
     * アクティブウィンドウ以外のすべてのウィンドウを閉じる。
     * ウィンドウツリーをアクティブウィンドウのみのLeafに置換する。
     */
    public void deleteOtherWindows() {
        windowTree = new WindowTree.Leaf(activeWindow);
    }

    /**
     * アクティブウィンドウを次のウィンドウに切り替える。
     * ウィンドウツリーの深さ優先順で循環する。
     * ミニバッファアクティブ中でもツリー内のウィンドウに移動する。
     * ウィンドウが1つしかない場合は何もしない。
     */
    public void nextWindow() {
        var windows = windowTree.windows();
        if (windows.size() <= 1) {
            return;
        }
        // ミニバッファアクティブ中はactiveWindowがミニバッファなので、
        // ツリーの最初のウィンドウに移動する
        if (activeWindow == minibufferWindow) {
            activeWindow = windows.get(0);
            return;
        }
        int index = windows.indexOf(activeWindow);
        int nextIndex = (index + 1) % windows.size();
        activeWindow = windows.get(nextIndex);
    }

    /**
     * Frameの現在の状態からimmutableな描画スナップショットを生成する。
     * ensurePointVisible等の副作用を伴う調整もここで行う。
     *
     * @param cols 画面カラム数
     * @param rows 画面行数
     * @param messageBuffer エコーエリア表示用のメッセージバッファ
     * @return 描画スナップショット
     */
    public RenderSnapshot createSnapshot(int cols, int rows, MessageBuffer messageBuffer) {
        int windowAreaRows = rows - 1;
        int minibufferRow = rows - 1;

        var treeArea = new Rect(0, 0, cols, windowAreaRows);
        var layoutResult = WindowLayout.compute(windowTree, treeArea);

        var windowSnapshots = Lists.mutable.<RenderSnapshot.WindowSnapshot>empty();

        layoutResult.windowRects().forEachKeyValue((window, rect) -> {
            if (rect.height() < 2) {
                return;
            }
            int bufferRows = rect.height() - 1;
            window.ensurePointVisible(bufferRows);
            window.ensurePointHorizontallyVisible(rect.width());

            var buffer = window.getBuffer();
            int lineCount = buffer.lineCount();
            int displayStart = window.getDisplayStartLine();
            int displayStartColumn = window.getDisplayStartColumn();
            var stylerOpt = buffer.getMajorMode().styler();

            var visibleLines = Lists.mutable.<RenderSnapshot.LineSnapshot>empty();
            if (stylerOpt.isPresent()) {
                SyntaxStyler styler = stylerOpt.get();
                StylingState styleState = styler.initialState();
                for (int i = 0; i < displayStart && i < lineCount; i++) {
                    styleState = styler.styleLineWithState(buffer.lineText(i), styleState)
                            .nextState();
                }
                for (int row = 0; row < bufferRows; row++) {
                    int lineIndex = displayStart + row;
                    if (lineIndex < lineCount) {
                        String lineText = buffer.lineText(lineIndex);
                        var result = styler.styleLineWithState(lineText, styleState);
                        visibleLines.add(new RenderSnapshot.LineSnapshot(lineText, Optional.of(result.spans())));
                        styleState = result.nextState();
                    }
                }
            } else {
                for (int row = 0; row < bufferRows; row++) {
                    int lineIndex = displayStart + row;
                    if (lineIndex < lineCount) {
                        String lineText = buffer.lineText(lineIndex);
                        visibleLines.add(new RenderSnapshot.LineSnapshot(lineText, Optional.empty()));
                    }
                }
            }

            String modeLine = buildModeLineText(window);
            windowSnapshots.add(new RenderSnapshot.WindowSnapshot(rect, visibleLines, displayStartColumn, modeLine));
        });

        // ミニバッファ / エコーエリア
        RenderSnapshot.MinibufferSnapshot minibufferSnapshot;
        if (minibufferActive) {
            minibufferWindow.ensurePointHorizontallyVisible(cols);
            var buffer = minibufferWindow.getBuffer();
            String text = buffer.length() > 0 ? buffer.getText() : "";
            minibufferSnapshot =
                    new RenderSnapshot.MinibufferSnapshot(Optional.of(text), minibufferWindow.getDisplayStartColumn());
        } else if (messageBuffer.isShowingMessage()) {
            var msg = messageBuffer.getLastMessage();
            minibufferSnapshot = new RenderSnapshot.MinibufferSnapshot(msg, 0);
        } else {
            minibufferSnapshot = new RenderSnapshot.MinibufferSnapshot(Optional.empty(), 0);
        }

        // カーソル位置
        int cursorColumn;
        int cursorRow;
        if (minibufferActive) {
            var mbCursor = computeMinibufferCursorPosition(minibufferWindow, minibufferRow, cols);
            cursorColumn = mbCursor[0];
            cursorRow = mbCursor[1];
        } else {
            var activeRect = layoutResult.windowRects().get(activeWindow);
            if (activeRect != null && activeRect.height() >= 2) {
                var cursor = computeCursorPosition(activeWindow, activeRect);
                cursorColumn = cursor[0];
                cursorRow = cursor[1];
            } else {
                cursorColumn = 0;
                cursorRow = 0;
            }
        }

        return new RenderSnapshot(
                cols, rows, windowSnapshots, layoutResult.separators(), minibufferSnapshot, cursorColumn, cursorRow);
    }

    private static String buildModeLineText(Window window) {
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int column = point - lineStart;

        String dirty = buffer.isDirty() ? "**" : "--";
        String bufferName = buffer.getName();
        String modeName = buffer.getMajorMode().name();
        return String.format("--%s  %s    (%d,%d)  (%s)", dirty, bufferName, lineIndex + 1, column, modeName);
    }

    /**
     * @return {column, row}
     */
    private static int[] computeCursorPosition(Window window, Rect rect) {
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int displayStart = window.getDisplayStartLine();
        int bufferRows = rect.height() - 1;

        int screenRow = lineIndex - displayStart;
        if (screenRow < 0 || screenRow >= bufferRows) {
            return new int[] {rect.left(), rect.top()};
        }

        int col = DisplayWidthUtil.computeColumnForOffset(buffer.lineText(lineIndex), point - lineStart);
        int screenCol = col - window.getDisplayStartColumn();
        if (screenCol >= 0 && screenCol < rect.width()) {
            return new int[] {rect.left() + screenCol, rect.top() + screenRow};
        }
        return new int[] {rect.left(), rect.top()};
    }

    /**
     * @return {column, row}
     */
    private static int[] computeMinibufferCursorPosition(Window minibufferWindow, int row, int maxColumns) {
        var buffer = minibufferWindow.getBuffer();
        int point = minibufferWindow.getPoint();
        String text = buffer.getText();
        int col = DisplayWidthUtil.computeColumnForOffset(text, point);
        int screenCol = col - minibufferWindow.getDisplayStartColumn();
        if (screenCol >= 0 && screenCol < maxColumns) {
            return new int[] {screenCol, row};
        }
        return new int[] {0, row};
    }

    /**
     * 指定バッファを表示中の全ウィンドウを代替バッファに切り替える。
     *
     * @param target      切り替え対象のバッファ
     * @param replacement 代替バッファ
     */
    public void replaceBufferInAllWindows(Buffer target, Buffer replacement) {
        for (var window : windowTree.windows()) {
            if (window.getBuffer().equals(target)) {
                window.setBuffer(replacement);
            }
        }
    }

    /**
     * 全ウィンドウの直前バッファが指定バッファの場合にクリアする。
     * バッファ削除時の dangling reference 防止用。
     */
    public void clearPreviousBufferInAllWindows(Buffer target) {
        for (var window : windowTree.windows()) {
            window.clearPreviousBufferIf(target);
        }
    }

    /**
     * 全ウィンドウで表示中のバッファ名のセットを返す。
     */
    public org.eclipse.collections.api.set.ImmutableSet<String> getDisplayedBufferNames() {
        return windowTree
                .windows()
                .collect(w -> w.getBuffer().getName())
                .toSet()
                .toImmutable();
    }

    /**
     * ツリーの最初のLeafに含まれるウィンドウを返す。
     */
    private static Window findFirstWindow(WindowTree tree) {
        return switch (tree) {
            case WindowTree.Leaf leaf -> leaf.window();
            case WindowTree.Split split -> findFirstWindow(split.first());
        };
    }
}
