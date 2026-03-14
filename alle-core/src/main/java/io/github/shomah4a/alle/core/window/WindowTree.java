package io.github.shomah4a.alle.core.window;

import java.util.Optional;

/**
 * ウィンドウの分割状態を表すimmutableな木構造。
 */
public sealed interface WindowTree {

    /**
     * 単一のウィンドウ。
     */
    record Leaf(Window window) implements WindowTree {}

    /**
     * 分割ノード。2つの子を持つ。
     *
     * @param direction 分割方向
     * @param ratio     first側の割合(0.0〜1.0)
     * @param first     1つ目の子(上または左)
     * @param second    2つ目の子(下または右)
     */
    record Split(Direction direction, double ratio, WindowTree first, WindowTree second) implements WindowTree {}

    /**
     * 指定ウィンドウを含むLeafを見つけて、分割した新しいツリーを返す。
     *
     * @param target    分割対象のウィンドウ
     * @param direction 分割方向
     * @param newWindow 新しく作成するウィンドウ
     * @return 分割後の新しいツリー。対象が見つからない場合はempty
     */
    default Optional<WindowTree> split(Window target, Direction direction, Window newWindow) {
        return switch (this) {
            case Leaf leaf -> {
                if (leaf.window() == target) {
                    yield Optional.of(new Split(direction, 0.5, new Leaf(target), new Leaf(newWindow)));
                }
                yield Optional.empty();
            }
            case Split split -> {
                var firstResult = split.first().split(target, direction, newWindow);
                if (firstResult.isPresent()) {
                    yield Optional.of(new Split(split.direction(), split.ratio(), firstResult.get(), split.second()));
                }
                var secondResult = split.second().split(target, direction, newWindow);
                if (secondResult.isPresent()) {
                    yield Optional.of(new Split(split.direction(), split.ratio(), split.first(), secondResult.get()));
                }
                yield Optional.empty();
            }
        };
    }

    /**
     * 指定ウィンドウを削除した新しいツリーを返す。
     * ツリーがLeaf1つだけの場合（最後のウィンドウ）はemptyを返す。
     *
     * @param target 削除対象のウィンドウ
     * @return 削除後の新しいツリー。削除不可の場合はempty
     */
    default Optional<WindowTree> remove(Window target) {
        return switch (this) {
            case Leaf leaf -> Optional.empty();
            case Split split -> {
                // firstが対象のLeafなら、secondを返す（縮退）
                if (split.first() instanceof Leaf firstLeaf && firstLeaf.window() == target) {
                    yield Optional.of(split.second());
                }
                // secondが対象のLeafなら、firstを返す（縮退）
                if (split.second() instanceof Leaf secondLeaf && secondLeaf.window() == target) {
                    yield Optional.of(split.first());
                }
                // first側の子ツリーから再帰的に削除を試みる
                var firstResult = split.first().remove(target);
                if (firstResult.isPresent()) {
                    yield Optional.of(new Split(split.direction(), split.ratio(), firstResult.get(), split.second()));
                }
                // second側の子ツリーから再帰的に削除を試みる
                var secondResult = split.second().remove(target);
                if (secondResult.isPresent()) {
                    yield Optional.of(new Split(split.direction(), split.ratio(), split.first(), secondResult.get()));
                }
                yield Optional.empty();
            }
        };
    }

    /**
     * ツリーに含まれるウィンドウを探す。
     */
    default boolean contains(Window target) {
        return switch (this) {
            case Leaf leaf -> leaf.window() == target;
            case Split split -> split.first().contains(target) || split.second().contains(target);
        };
    }
}
