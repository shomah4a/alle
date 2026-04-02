package io.github.shomah4a.alle.core.buffer;

import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

/**
 * 複数のバッファを管理する。
 */
public class BufferManager {

    private final MutableList<BufferFacade> buffers;
    private final BufferNameUniquifier uniquifier;
    private int currentIndex;

    public BufferManager() {
        this.buffers = Lists.mutable.empty();
        this.uniquifier = new BufferNameUniquifier();
        this.currentIndex = -1;
    }

    /**
     * バッファを追加し、現在のバッファとして設定する。
     */
    public void add(BufferFacade buffer) {
        buffers.add(buffer);
        currentIndex = buffers.size() - 1;
        uniquifier.uniquify(buffers);
    }

    /**
     * 現在のバッファを返す。
     */
    public Optional<BufferFacade> current() {
        if (currentIndex < 0 || currentIndex >= buffers.size()) {
            return Optional.empty();
        }
        return Optional.of(buffers.get(currentIndex));
    }

    /**
     * 名前でバッファを検索する。
     */
    public Optional<BufferFacade> findByName(String name) {
        return Optional.ofNullable(buffers.detect(b -> b.getName().equals(name)));
    }

    /**
     * ファイルパスでバッファを検索する。
     */
    public Optional<BufferFacade> findByPath(Path path) {
        return Optional.ofNullable(
                buffers.detect(b -> b.getFilePath().map(p -> p.equals(path)).orElse(false)));
    }

    /**
     * 指定バッファを現在のバッファに切り替える。
     *
     * @return 切り替えに成功した場合true
     */
    public boolean switchTo(String name) {
        for (int i = 0; i < buffers.size(); i++) {
            if (buffers.get(i).getName().equals(name)) {
                currentIndex = i;
                return true;
            }
        }
        return false;
    }

    /**
     * 指定バッファを削除する。
     * 現在のバッファが削除された場合、前のバッファに切り替える。
     *
     * @return 削除に成功した場合true
     */
    public boolean remove(String name) {
        for (int i = 0; i < buffers.size(); i++) {
            if (buffers.get(i).getName().equals(name)) {
                buffers.remove(i);
                if (buffers.isEmpty()) {
                    currentIndex = -1;
                } else if (currentIndex >= buffers.size()) {
                    currentIndex = buffers.size() - 1;
                } else if (currentIndex > i) {
                    currentIndex--;
                }
                uniquifier.uniquify(buffers);
                return true;
            }
        }
        return false;
    }

    /**
     * 全バッファの読み取り専用リストを返す。
     */
    public ListIterable<BufferFacade> getBuffers() {
        return buffers;
    }

    /**
     * バッファ名のuniquifyを再計算する。
     * バッファのファイルパスが変更された場合に呼び出す。
     */
    public void recomputeUniquify() {
        uniquifier.uniquify(buffers);
    }

    /**
     * バッファ数を返す。
     */
    public int size() {
        return buffers.size();
    }
}
