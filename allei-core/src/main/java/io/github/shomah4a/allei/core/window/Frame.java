package io.github.shomah4a.allei.core.window;

import io.github.shomah4a.allei.core.buffer.Buffer;

/**
 * エディタのフレーム。
 * ターミナル(TUI)またはOSウィンドウ(GUI)1つに対応する。
 * ウィンドウツリーとミニバッファウィンドウを保持し、ウィンドウの分割・削除・切り替えを管理する。
 */
public class Frame {

    private WindowTree windowTree;
    private final Window minibufferWindow;
    private Window activeWindow;

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
     * ウィンドウツリーに含まれるウィンドウのみ設定可能。
     *
     * @throws IllegalArgumentException ウィンドウがツリーに含まれない場合
     */
    public void setActiveWindow(Window window) {
        if (!windowTree.contains(window)) {
            throw new IllegalArgumentException("Window is not in the window tree");
        }
        this.activeWindow = window;
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
     * ツリーの最初のLeafに含まれるウィンドウを返す。
     */
    private static Window findFirstWindow(WindowTree tree) {
        return switch (tree) {
            case WindowTree.Leaf leaf -> leaf.window();
            case WindowTree.Split split -> findFirstWindow(split.first());
        };
    }
}
