package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * 既存コマンドに別名を与えるためのラッパー。
 * 指定した名前で登録し、実行時は委譲先コマンドをそのまま実行する。
 */
public class CommandAlias implements Command {

    private final String name;
    private final Command delegate;

    public CommandAlias(String name, Command delegate) {
        this.name = name;
        this.delegate = delegate;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return delegate.execute(context);
    }

    @Override
    public boolean keepsRegionActive() {
        return delegate.keepsRegionActive();
    }
}
