package io.github.shomah4a.alle.core.window;

import io.github.shomah4a.alle.core.buffer.Buffer;

/**
 * エディタのフレーム。
 * ターミナル(TUI)またはOSウィンドウ(GUI)1つに対応する。
 * ウィンドウツリーとミニバッファウィンドウを保持し、ウィンドウの分割・削除・切り替えを管理する。
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
     * ツリーの最初のLeafに含まれるウィンドウを返す。
     */
    private static Window findFirstWindow(WindowTree tree) {
        return switch (tree) {
            case WindowTree.Leaf leaf -> leaf.window();
            case WindowTree.Split split -> findFirstWindow(split.first());
        };
    }
}
