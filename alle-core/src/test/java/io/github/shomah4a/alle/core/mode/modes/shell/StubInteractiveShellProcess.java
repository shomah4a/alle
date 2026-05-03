package io.github.shomah4a.alle.core.mode.modes.shell;

import io.github.shomah4a.alle.core.input.InteractiveShellProcess;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

/**
 * テスト用の {@link InteractiveShellProcess} スタブ。
 * sendInput/sendSignal の呼び出しを記録し、isAlive の状態を制御可能。
 */
class StubInteractiveShellProcess implements InteractiveShellProcess {

    private final MutableList<String> sentInputs = Lists.mutable.empty();
    private final MutableList<Integer> sentSignals = Lists.mutable.empty();
    private boolean alive = true;

    @Override
    public void sendInput(String input) {
        sentInputs.add(input);
    }

    @Override
    public void sendSignal(int signal) {
        sentSignals.add(signal);
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void destroy() {
        alive = false;
    }

    @Override
    public long pid() {
        return 12345;
    }

    MutableList<String> getSentInputs() {
        return sentInputs;
    }

    MutableList<Integer> getSentSignals() {
        return sentSignals;
    }

    void setAlive(boolean alive) {
        this.alive = alive;
    }
}
