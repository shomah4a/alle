package io.github.shomah4a.alle.libs.ringbuffer;

/**
 * 固定容量のリングバッファ。
 * 容量を超えて要素が追加されると、最古の要素から順に上書きされる。
 *
 * @param <T> 要素の型（非null）
 */
public interface RingBuffer<T> extends Iterable<T> {

    /**
     * 要素を末尾に追加する。
     * 容量超過時は最古の要素を上書きする。
     *
     * @throws NullPointerException elementがnullの場合
     */
    void add(T element);

    /**
     * 指定インデックスの要素を返す。
     * インデックス0が最古の要素。
     *
     * @throws IndexOutOfBoundsException インデックスが範囲外の場合
     */
    T get(int index);

    /**
     * 現在の要素数を返す。
     */
    int size();

    /**
     * 最大容量を返す。
     */
    int capacity();

    /**
     * 要素が空かどうかを返す。
     */
    boolean isEmpty();

    /**
     * 全要素をクリアする。
     */
    void clear();
}
