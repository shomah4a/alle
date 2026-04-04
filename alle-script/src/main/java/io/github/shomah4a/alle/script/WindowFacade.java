package io.github.shomah4a.alle.script;

import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;

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

    /**
     * リージョンの開始位置を返す。markが未設定の場合は-1を返す。
     */
    public int regionStart() {
        return window.getRegionStart().orElse(-1);
    }

    /**
     * リージョンの終了位置を返す。markが未設定の場合は-1を返す。
     */
    public int regionEnd() {
        return window.getRegionEnd().orElse(-1);
    }

    /**
     * markの位置を返す。未設定の場合は-1を返す。
     */
    public int mark() {
        return window.getMark().orElse(-1);
    }

    /**
     * markを設定する。
     */
    public void setMark(int position) {
        window.setMark(position);
    }

    /**
     * 選択中のテキストを返す。markが未設定の場合は空のOptionalを返す。
     */
    public Optional<String> selectedText() {
        var start = window.getRegionStart();
        var end = window.getRegionEnd();
        if (start.isEmpty() || end.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(window.getBuffer().substring(start.get(), end.get()));
    }
}
