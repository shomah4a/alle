package io.github.shomah4a.allei.core.buffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 複数のバッファを管理する。
 */
public class BufferManager {

    private final List<Buffer> buffers;
    private int currentIndex;

    public BufferManager() {
        this.buffers = new ArrayList<>();
        this.currentIndex = -1;
    }

    /**
     * バッファを追加し、現在のバッファとして設定する。
     */
    public void add(Buffer buffer) {
        buffers.add(buffer);
        currentIndex = buffers.size() - 1;
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
        return buffers.stream()
                .filter(b -> b.getName().equals(name))
                .findFirst();
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
                return true;
            }
        }
        return false;
    }

    /**
     * 全バッファの読み取り専用リストを返す。
     */
    public List<Buffer> getBuffers() {
        return Collections.unmodifiableList(buffers);
    }

    /**
     * バッファ数を返す。
     */
    public int size() {
        return buffers.size();
    }
}
