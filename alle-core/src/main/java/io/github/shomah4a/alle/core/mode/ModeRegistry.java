package io.github.shomah4a.alle.core.mode;

import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * メジャーモード・マイナーモードを名前で登録・検索するレジストリ。
 * スクリプトからのモード登録の基盤となる。
 */
public class ModeRegistry {

    private final MutableMap<String, Supplier<MajorMode>> majorModes = Maps.mutable.empty();
    private final MutableMap<String, Supplier<MinorMode>> minorModes = Maps.mutable.empty();

    /**
     * メジャーモードファクトリを登録する。
     *
     * @throws IllegalStateException 同名のモードが既に登録されている場合
     */
    public void registerMajorMode(String name, Supplier<MajorMode> factory) {
        if (majorModes.containsKey(name)) {
            throw new IllegalStateException("メジャーモード '" + name + "' は既に登録されています");
        }
        majorModes.put(name, factory);
    }

    /**
     * メジャーモードファクトリを登録する。同名のモードが既に登録されている場合は上書きする。
     * スクリプトからのモード再定義用。
     */
    public void registerOrReplaceMajorMode(String name, Supplier<MajorMode> factory) {
        majorModes.put(name, factory);
    }

    /**
     * 名前でメジャーモードファクトリを検索する。
     */
    public Optional<Supplier<MajorMode>> lookupMajorMode(String name) {
        return Optional.ofNullable(majorModes.get(name));
    }

    /**
     * 登録済みメジャーモード名の一覧を返す。
     */
    public ImmutableSet<String> registeredMajorModeNames() {
        return majorModes.keysView().toImmutableSet();
    }

    /**
     * マイナーモードファクトリを登録する。
     *
     * @throws IllegalStateException 同名のモードが既に登録されている場合
     */
    public void registerMinorMode(String name, Supplier<MinorMode> factory) {
        if (minorModes.containsKey(name)) {
            throw new IllegalStateException("マイナーモード '" + name + "' は既に登録されています");
        }
        minorModes.put(name, factory);
    }

    /**
     * マイナーモードファクトリを登録する。同名のモードが既に登録されている場合は上書きする。
     * スクリプトからのモード再定義用。
     */
    public void registerOrReplaceMinorMode(String name, Supplier<MinorMode> factory) {
        minorModes.put(name, factory);
    }

    /**
     * 名前でマイナーモードファクトリを検索する。
     */
    public Optional<Supplier<MinorMode>> lookupMinorMode(String name) {
        return Optional.ofNullable(minorModes.get(name));
    }

    /**
     * 登録済みマイナーモード名の一覧を返す。
     */
    public ImmutableSet<String> registeredMinorModeNames() {
        return minorModes.keysView().toImmutableSet();
    }
}
