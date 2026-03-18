package io.github.shomah4a.alle.libs.ringbuffer;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * {@link RingBuffer}の配列ベース実装。
 * 内部的に循環配列を使用し、先頭・末尾の操作がO(1)で行える。
 *
 * @param <T> 要素の型（非null）
 */
public class ArrayRingBuffer<T> implements RingBuffer<T> {

    // Javaのジェネリクスは型消去により new T[] が書けないため Object[] を使用
    private final Object[] array;
    private int head;
    private int size;

    /**
     * 指定容量のリングバッファを作成する。
     *
     * @param capacity 最大容量（1以上）
     * @throws IllegalArgumentException capacityが1未満の場合
     */
    public ArrayRingBuffer(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1, but was " + capacity);
        }
        this.array = new Object[capacity];
        this.head = 0;
        this.size = 0;
    }

    @Override
    public void add(T element) {
        int index = (head + size) % array.length;
        array[index] = element;
        if (size == array.length) {
            head = (head + 1) % array.length;
        } else {
            size++;
        }
    }

    @Override
    // 型消去により Object[] → T のキャストが不可避 (add で T のみ格納するため安全)
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + " is out of bounds [0, " + size + ")");
        }
        return (T) array[(head + index) % array.length];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int capacity() {
        return array.length;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            array[(head + i) % array.length] = null;
        }
        head = 0;
        size = 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new RingBufferIterator();
    }

    private class RingBufferIterator implements Iterator<T> {
        private int current = 0;

        @Override
        public boolean hasNext() {
            return current < size;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return get(current++);
        }
    }
}
