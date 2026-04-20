package io.github.shomah4a.alle.core.statusline;

/**
 * ステータスラインの末端スロット。
 * 名前付きのハンドラで、コンテキストから表示文字列を生成する。
 */
public final class StatusLineSlot implements StatusLineElement {

    private final String name;
    private final StatusLineHandler handler;

    public StatusLineSlot(String name, StatusLineHandler handler) {
        this.name = name;
        this.handler = handler;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String render(StatusLineContext context) {
        return handler.render(context);
    }

    /**
     * スロットのハンドラ。コンテキストを受け取り表示文字列を返す関数型インターフェース。
     */
    @FunctionalInterface
    public interface StatusLineHandler {
        String render(StatusLineContext context);
    }
}
