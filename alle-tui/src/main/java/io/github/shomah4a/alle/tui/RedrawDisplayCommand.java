package io.github.shomah4a.alle.tui;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 全画面強制再描画コマンド。
 * EmacsのC-lに相当し、ターミナル画面を完全に再描画する。
 * AtomicBooleanフラグをセットし、RenderThreadが次回描画時に
 * RefreshType.COMPLETEを使用するよう通知する。
 */
public class RedrawDisplayCommand implements Command {

    private final AtomicBoolean fullRedrawRequested;

    public RedrawDisplayCommand(AtomicBoolean fullRedrawRequested) {
        this.fullRedrawRequested = fullRedrawRequested;
    }

    @Override
    public String name() {
        return "redraw-display";
    }

    @Override
    public boolean keepsRegionActive() {
        return true;
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        fullRedrawRequested.set(true);
        return CompletableFuture.completedFuture(null);
    }
}
