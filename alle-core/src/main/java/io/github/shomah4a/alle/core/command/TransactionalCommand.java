package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * 複数のバッファ編集を1つのundo単位にまとめるコマンド。
 * {@link #execute} はトランザクションを開始してから {@link #executeInTransaction} を呼び出す。
 * コマンド実装者は {@link #executeInTransaction} のみを実装する。
 */
public interface TransactionalCommand extends Command {

    /**
     * トランザクション内でコマンドを実行する。
     * この中で行われるバッファ編集はすべて1つのundo単位にまとまる。
     */
    CompletableFuture<Void> executeInTransaction(CommandContext context);

    @Override
    default CompletableFuture<Void> execute(CommandContext context) {
        var buffer = context.activeWindow().getBuffer();
        return buffer.getUndoManager().withTransaction(() -> executeInTransaction(context));
    }
}
