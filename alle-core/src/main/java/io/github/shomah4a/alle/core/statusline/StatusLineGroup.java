package io.github.shomah4a.alle.core.statusline;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

/**
 * ステータスラインのグループ（Compositeノード）。
 * 子要素のリストを持ち、各子要素のrender結果を連結して返す。
 * 汎用スロットの追加はグループへの子要素追加で行い、
 * フォーマット定義を変更せずに全モードに波及させる。
 */
public final class StatusLineGroup implements StatusLineElement {

    private final String name;
    private final MutableList<StatusLineElement> children = Lists.mutable.empty();

    public StatusLineGroup(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String render(StatusLineContext context) {
        var sb = new StringBuilder();
        for (StatusLineElement child : children) {
            sb.append(child.render(context));
        }
        return sb.toString();
    }

    /**
     * 子要素を末尾に追加する。
     * レジストリ登録後も呼び出し可能。汎用スロット（git-status等）の後付け追加に使用する。
     */
    public void addChild(StatusLineElement child) {
        children.add(child);
    }

    /**
     * 子要素のリストを返す。
     */
    public ListIterable<StatusLineElement> children() {
        return children;
    }
}
