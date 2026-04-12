package io.github.shomah4a.alle.core.statusline;

/**
 * ステータスラインの構成要素。
 * 末端のスロットとグループ（Composite）の共通インターフェース。
 */
public interface StatusLineElement {

    /**
     * このスロットの名前を返す。レジストリへの登録キーとして使用される。
     */
    String name();

    /**
     * コンテキストを受け取り、表示文字列を返す。
     */
    String render(StatusLineContext context);
}
