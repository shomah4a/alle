package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.input.DirectoryEntry;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;

/**
 * ツリー表示のディレクトリ状態を管理するモデル。
 * 展開済みディレクトリのセットを保持し、表示すべきエントリのリストを生成する。
 */
public class TreeDiredModel {

    private static final Logger logger = Logger.getLogger(TreeDiredModel.class.getName());

    private Path rootDirectory;
    private final MutableSet<Path> expandedDirectories;
    private final DirectoryLister directoryLister;

    public TreeDiredModel(Path rootDirectory, DirectoryLister directoryLister) {
        this.rootDirectory = rootDirectory;
        this.expandedDirectories = Sets.mutable.empty();
        this.directoryLister = directoryLister;
        expandedDirectories.add(rootDirectory);
    }

    /**
     * ルートディレクトリを返す。
     */
    public Path getRootDirectory() {
        return rootDirectory;
    }

    /**
     * ルートディレクトリを変更する。
     * 展開状態はリセットされ、新しいルートのみ展開された状態になる。
     */
    public void setRootDirectory(Path newRoot) {
        this.rootDirectory = newRoot;
        expandedDirectories.clear();
        expandedDirectories.add(newRoot);
    }

    /**
     * 指定ディレクトリの展開/折り畳みを切り替える。
     * ルートディレクトリは折り畳めない。
     */
    public void toggle(Path directory) {
        if (directory.equals(rootDirectory)) {
            return;
        }
        if (expandedDirectories.contains(directory)) {
            expandedDirectories.remove(directory);
        } else {
            expandedDirectories.add(directory);
        }
    }

    /**
     * 指定ディレクトリが展開済みかどうかを返す。
     */
    public boolean isExpanded(Path directory) {
        return expandedDirectories.contains(directory);
    }

    /**
     * 現在の展開状態に基づき、表示すべきエントリのリストを生成する。
     * エントリはディレクトリ先・名前順でソートされる。
     */
    public ListIterable<TreeDiredEntry> getVisibleEntries() {
        MutableList<TreeDiredEntry> entries = Lists.mutable.empty();
        collectEntries(rootDirectory, 0, entries);
        return entries;
    }

    private void collectEntries(Path directory, int depth, MutableList<TreeDiredEntry> entries) {
        ListIterable<DirectoryEntry> children;
        try {
            children = directoryLister.list(directory);
        } catch (IOException e) {
            logger.log(Level.FINE, "ディレクトリの読み取りに失敗: " + directory, e);
            return;
        }

        MutableList<DirectoryEntry> sorted = Lists.mutable.withAll(children);
        sorted.sortThis((a, b) -> {
            boolean aIsDir = a instanceof DirectoryEntry.Directory;
            boolean bIsDir = b instanceof DirectoryEntry.Directory;
            if (aIsDir != bIsDir) {
                return aIsDir ? -1 : 1;
            }
            return a.path()
                    .getFileName()
                    .toString()
                    .compareToIgnoreCase(b.path().getFileName().toString());
        });

        for (DirectoryEntry child : sorted) {
            boolean isDir = child instanceof DirectoryEntry.Directory;
            boolean expanded = isDir && expandedDirectories.contains(child.path());
            entries.add(new TreeDiredEntry(child.path(), depth, isDir, expanded, child.attributes()));
            if (expanded) {
                collectEntries(child.path(), depth + 1, entries);
            }
        }
    }
}
