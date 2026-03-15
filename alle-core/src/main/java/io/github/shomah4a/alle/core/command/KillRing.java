package io.github.shomah4a.alle.core.command;

import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

/**
 * killコマンドで削除されたテキストを蓄積するリングバッファ。
 * Emacsのkill-ringに相当する。
 */
public class KillRing {

    private static final int DEFAULT_MAX_SIZE = 60;

    private final int maxSize;
    private final MutableList<String> entries;
    private int index;

    public KillRing() {
        this(DEFAULT_MAX_SIZE);
    }

    public KillRing(int maxSize) {
        this.maxSize = maxSize;
        this.entries = Lists.mutable.empty();
        this.index = -1;
    }

    /**
     * 新しいエントリを追加する。
     * 最大サイズを超えた場合は最も古いエントリが削除される。
     */
    public void push(String text) {
        entries.add(0, text);
        if (entries.size() > maxSize) {
            entries.remove(entries.size() - 1);
        }
        index = 0;
    }

    /**
     * 最新エントリの末尾にテキストを追記する。
     * 連続killで前回の削除テキストと結合するために使用する。
     * エントリが空の場合はpushと同じ動作をする。
     */
    public void appendToLast(String text) {
        if (entries.isEmpty()) {
            push(text);
            return;
        }
        entries.set(0, entries.get(0) + text);
        index = 0;
    }

    /**
     * 現在のエントリを返す。
     */
    public Optional<String> current() {
        if (entries.isEmpty() || index < 0 || index >= entries.size()) {
            return Optional.empty();
        }
        return Optional.of(entries.get(index));
    }

    /**
     * エントリ数を返す。
     */
    public int size() {
        return entries.size();
    }
}
