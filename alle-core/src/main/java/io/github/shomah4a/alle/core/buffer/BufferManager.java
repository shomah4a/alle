package io.github.shomah4a.alle.core.buffer;

import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;

/**
 * 複数のバッファを管理する。
 */
public class BufferManager {

    private final MutableList<Buffer> buffers;
    private final MutableMap<Buffer, BufferActor> actorMap;
    private int currentIndex;

    public BufferManager() {
        this.buffers = Lists.mutable.empty();
        this.actorMap = Maps.mutable.empty();
        this.currentIndex = -1;
    }

    /**
     * バッファを追加し、現在のバッファとして設定する。
     */
    public void add(Buffer buffer) {
        buffers.add(buffer);
        actorMap.put(buffer, new BufferActor(buffer));
        currentIndex = buffers.size() - 1;
    }

    /**
     * 指定バッファに対応するBufferActorを返す。
     *
     * @throws IllegalArgumentException バッファが管理下にない場合
     */
    public BufferActor getActor(Buffer buffer) {
        var actor = actorMap.get(buffer);
        if (actor == null) {
            throw new IllegalArgumentException("管理下にないバッファです: " + buffer.getName());
        }
        return actor;
    }

    /**
     * 現在のバッファを返す。
     */
    public Optional<Buffer> current() {
        if (currentIndex < 0 || currentIndex >= buffers.size()) {
            return Optional.empty();
        }
        return Optional.of(buffers.get(currentIndex));
    }

    /**
     * 名前でバッファを検索する。
     */
    public Optional<Buffer> findByName(String name) {
        return Optional.ofNullable(buffers.detect(b -> b.getName().equals(name)));
    }

    /**
     * ファイルパスでバッファを検索する。
     */
    public Optional<Buffer> findByPath(Path path) {
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
                actorMap.remove(buffers.get(i));
                buffers.remove(i);
                if (buffers.isEmpty()) {
                    currentIndex = -1;
                } else if (currentIndex >= buffers.size()) {
                    currentIndex = buffers.size() - 1;
                } else if (currentIndex > i) {
                    currentIndex--;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 全バッファの読み取り専用リストを返す。
     */
    public ListIterable<Buffer> getBuffers() {
        return buffers;
    }

    /**
     * バッファ数を返す。
     */
    public int size() {
        return buffers.size();
    }
}
