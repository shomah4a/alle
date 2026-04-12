package io.github.shomah4a.alle.core.statusline;

import java.util.Optional;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * ステータスラインの名前付きエレメントのレジストリ。
 * 名前 → StatusLineElement のフラットなマッピングを管理する。
 */
public class StatusLineRegistry {

    private final MutableMap<String, StatusLineElement> elements = Maps.mutable.empty();

    /**
     * エレメントを登録する。
     *
     * @throws IllegalStateException 同名のエレメントが既に登録されている場合
     */
    public void register(StatusLineElement element) {
        String name = element.name();
        if (elements.containsKey(name)) {
            throw new IllegalStateException("ステータスラインエレメント '" + name + "' は既に登録されています");
        }
        elements.put(name, element);
    }

    /**
     * 名前でエレメントを検索する。
     */
    public Optional<StatusLineElement> lookup(String name) {
        return Optional.ofNullable(elements.get(name));
    }

    /**
     * 登録済みエレメント名の一覧を返す。
     */
    public ImmutableSet<String> registeredNames() {
        return elements.keysView().toImmutableSet();
    }
}
