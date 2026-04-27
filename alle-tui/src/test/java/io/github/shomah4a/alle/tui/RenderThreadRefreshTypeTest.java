package io.github.shomah4a.alle.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.googlecode.lanterna.screen.Screen;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class RenderThreadRefreshTypeTest {

    @Test
    void フラグがfalseのときDELTAを返す() {
        var flag = new AtomicBoolean(false);

        assertEquals(Screen.RefreshType.DELTA, RenderThread.resolveRefreshType(flag));
    }

    @Test
    void フラグがtrueのときCOMPLETEを返しフラグを消費する() {
        var flag = new AtomicBoolean(true);

        assertEquals(Screen.RefreshType.COMPLETE, RenderThread.resolveRefreshType(flag));
        assertEquals(Screen.RefreshType.DELTA, RenderThread.resolveRefreshType(flag));
    }

    @Test
    void 連続呼び出しで再セット後にCOMPLETEを返す() {
        var flag = new AtomicBoolean(false);

        assertEquals(Screen.RefreshType.DELTA, RenderThread.resolveRefreshType(flag));

        flag.set(true);
        assertEquals(Screen.RefreshType.COMPLETE, RenderThread.resolveRefreshType(flag));
        assertEquals(Screen.RefreshType.DELTA, RenderThread.resolveRefreshType(flag));
    }
}
