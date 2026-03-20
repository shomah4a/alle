package io.github.shomah4a.alle.script.graalpy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Python側でJavaインターフェースを実装できるかの検証。
 */
class PythonJavaInterfaceTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder("python").allowAllAccess(true).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void PythonクラスでJavaインターフェースを継承して実装できる() {
        context.eval("python", """
                import java
                Command = java.type('io.github.shomah4a.alle.core.command.Command')

                class MyCommand(Command):
                    def name(self):
                        return "my-command"
                    def execute(self, ctx):
                        from java.util.concurrent import CompletableFuture
                        return CompletableFuture.completedFuture(None)
                """);
        Value pyCmd = context.eval("python", "MyCommand()");
        Command cmd = pyCmd.as(Command.class);
        assertEquals("my-command", cmd.name());
    }

    @Test
    void PythonクラスでMajorModeを実装できる() {
        context.eval("python", """
                import java
                MajorMode = java.type('io.github.shomah4a.alle.core.mode.MajorMode')

                class MyMode(MajorMode):
                    def name(self):
                        return "my-mode"
                    def keymap(self):
                        from java.util import Optional
                        return Optional.empty()
                """);
        Value pyMode = context.eval("python", "MyMode()");
        MajorMode mode = pyMode.as(MajorMode.class);
        assertEquals("my-mode", mode.name());
        assertTrue(mode.keymap().isEmpty());
    }

    @Test
    void PythonクラスでMinorModeを実装できる() {
        context.eval("python", """
                import java
                MinorMode = java.type('io.github.shomah4a.alle.core.mode.MinorMode')

                class MyMinor(MinorMode):
                    def name(self):
                        return "my-minor"
                    def keymap(self):
                        from java.util import Optional
                        return Optional.empty()
                """);
        Value pyMode = context.eval("python", "MyMinor()");
        MinorMode mode = pyMode.as(MinorMode.class);
        assertEquals("my-minor", mode.name());
    }
}
