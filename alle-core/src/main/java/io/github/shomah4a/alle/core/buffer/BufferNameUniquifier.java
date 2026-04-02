package io.github.shomah4a.alle.core.buffer;

import java.nio.file.Path;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;

/**
 * 同名ファイルのバッファ名をディレクトリパスで区別するuniquify機能。
 * Emacsのforward style uniquifyに相当する。
 *
 * <p>同名ファイルが複数開かれている場合、ディレクトリパスの最長共通プレフィックスを除いた
 * 相対パスをバッファ名として設定する。衝突がなくなった場合はファイル名のみに戻す。</p>
 */
class BufferNameUniquifier {

    /**
     * バッファリスト全体に対してuniquifyを実行する。
     * ファイルパスを持つバッファをファイル名でグループ化し、
     * 同名グループが2個以上の場合にdisplayNameを設定する。
     */
    void uniquify(ListIterable<BufferFacade> buffers) {
        MutableMap<String, MutableList<BufferFacade>> groups = Maps.mutable.empty();

        for (BufferFacade buffer : buffers) {
            buffer.getFilePath().ifPresent(path -> {
                String fileName = path.getFileName().toString();
                groups.getIfAbsentPut(fileName, Lists.mutable::empty).add(buffer);
            });
        }

        groups.forEachKeyValue((fileName, group) -> {
            if (group.size() <= 1) {
                for (BufferFacade buffer : group) {
                    buffer.resetDisplayName();
                }
            } else {
                uniquifyGroup(group);
            }
        });
    }

    private void uniquifyGroup(MutableList<BufferFacade> group) {
        MutableList<Path> parents =
                group.collect(buffer -> buffer.getFilePath().orElseThrow().getParent());

        Path commonPrefix = computeCommonPrefix(parents);

        for (BufferFacade buffer : group) {
            Path filePath = buffer.getFilePath().orElseThrow();
            Path relativePath = commonPrefix.relativize(filePath);
            buffer.setDisplayName(relativePath.toString());
        }
    }

    /**
     * 複数のパスの最長共通プレフィックス（ディレクトリ単位）を計算する。
     */
    static Path computeCommonPrefix(ListIterable<Path> paths) {
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("paths must not be empty");
        }

        Path first = paths.get(0).normalize();
        int commonCount = first.getNameCount();

        for (int i = 1; i < paths.size(); i++) {
            Path other = paths.get(i).normalize();
            commonCount = Math.min(commonCount, other.getNameCount());
            for (int j = 0; j < commonCount; j++) {
                if (!first.getName(j).equals(other.getName(j))) {
                    commonCount = j;
                    break;
                }
            }
        }

        if (commonCount == 0) {
            return first.getRoot() != null ? first.getRoot() : Path.of("");
        }
        Path prefix = first.getRoot() != null
                ? first.getRoot().resolve(first.subpath(0, commonCount))
                : first.subpath(0, commonCount);
        return prefix;
    }
}
