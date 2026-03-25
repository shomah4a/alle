package io.github.shomah4a.alle.script;

import io.github.shomah4a.alle.core.window.Window;

/**
 * スクリプトに公開するウィンドウ操作のファサード。
 * Window経由で同期的に操作する。
 */
public class WindowFacade {

    private final Window window;

    public WindowFacade(Window window) {
        this.window = window;
    }

    /**
     * カーソル位置を返す。
     */
    public int point() {
        return window.getPoint();
    }

    /**
     * カーソルを指定位置に移動する。
     */
    public void gotoChar(int position) {
        window.setPoint(position);
    }

    /**
     * カーソル位置にテキストを挿入する。
     */
    public void insert(String text) {
        window.insert(text);
    }

    /**
     * カーソル位置から前方にcount文字削除する。
     */
    public void deleteBackward(int count) {
        window.deleteBackward(count);
    }

    /**
     * カーソル位置から後方にcount文字削除する。
     */
    public void deleteForward(int count) {
        window.deleteForward(count);
    }

    /**
     * このウィンドウのバッファのファサードを返す。
     */
    public BufferFacade buffer() {
        return new BufferFacade(window.getBuffer());
    }
}
