package io.github.shomah4a.alle.core.command;

import java.util.Optional;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * コマンドを名前で登録・検索するレジストリ。
 * 将来のスクリプト評価基盤からのコマンド呼び出しの基盤となる。
 */
public class CommandRegistry {

    private final MutableMap<String, Command> commands = Maps.mutable.empty();

    /**
     * コマンドを登録する。command.name()をキーとして使用する。
     *
     * @throws IllegalStateException 同名のコマンドが既に登録されている場合
     */
    public void register(Command command) {
        String name = command.name();
        if (commands.containsKey(name)) {
            throw new IllegalStateException("コマンド '" + name + "' は既に登録されています");
        }
        commands.put(name, command);
    }

    /**
     * 名前でコマンドを検索する。
     */
    public Optional<Command> lookup(String name) {
        return Optional.ofNullable(commands.get(name));
    }

    /**
     * 登録済みコマンド名の一覧を返す。
     */
    public ImmutableSet<String> registeredNames() {
        return commands.keysView().toImmutableSet();
    }
}
