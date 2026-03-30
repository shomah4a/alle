package io.github.shomah4a.alle.core.command;

import java.util.Optional;
import java.util.regex.Pattern;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * コマンドを名前で登録・検索するレジストリ。
 * 将来のスクリプト評価基盤からのコマンド呼び出しの基盤となる。
 */
public class CommandRegistry {

    private static final Pattern VALID_COMMAND_NAME = Pattern.compile("[A-Za-z0-9-]+");

    private final MutableMap<String, Command> commands = Maps.mutable.empty();

    /**
     * コマンド名が命名規約に準拠しているか検証する。
     * コマンド名に使用可能な文字は {@code [A-Za-z0-9-]} のみ。
     * {@code .} はFQCN区切り文字として予約されている。
     *
     * @throws IllegalArgumentException コマンド名が不正な場合
     */
    private static void validateCommandName(String name) {
        if (!VALID_COMMAND_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("コマンド名 '" + name + "' は不正です。使用可能な文字は [A-Za-z0-9-] のみです");
        }
    }

    /**
     * コマンドを登録する。command.name()をキーとして使用する。
     *
     * @throws IllegalStateException 同名のコマンドが既に登録されている場合
     * @throws IllegalArgumentException コマンド名が命名規約に準拠していない場合
     */
    public void register(Command command) {
        String name = command.name();
        validateCommandName(name);
        if (commands.containsKey(name)) {
            throw new IllegalStateException("コマンド '" + name + "' は既に登録されています");
        }
        commands.put(name, command);
    }

    /**
     * コマンドを登録する。同名のコマンドが既に登録されている場合は上書きする。
     * スクリプトからのコマンド再定義用。
     *
     * @throws IllegalArgumentException コマンド名が命名規約に準拠していない場合
     */
    public void registerOrReplace(Command command) {
        validateCommandName(command.name());
        commands.put(command.name(), command);
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
