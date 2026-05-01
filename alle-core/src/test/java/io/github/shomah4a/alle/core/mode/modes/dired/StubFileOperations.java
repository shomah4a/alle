package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.input.FileOperations;
import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

/**
 * テスト用のFileOperationsスタブ。
 * 呼び出された操作を記録する。
 */
class StubFileOperations implements FileOperations {

    final MutableList<String> operations = Lists.mutable.empty();

    @Override
    public void copy(Path source, Path target) throws IOException {
        operations.add("copy:" + source + "->" + target);
    }

    @Override
    public void move(Path source, Path target) throws IOException {
        operations.add("move:" + source + "->" + target);
    }

    @Override
    public void delete(Path path) throws IOException {
        operations.add("delete:" + path);
    }

    @Override
    public void setOwner(Path path, String owner) throws IOException {
        operations.add("chown:" + path + ":" + owner);
    }

    @Override
    public void setPermissions(Path path, String permissions) throws IOException {
        operations.add("chmod:" + path + ":" + permissions);
    }

    @Override
    public void createDirectories(Path path) throws IOException {
        operations.add("mkdir:" + path);
    }
}
