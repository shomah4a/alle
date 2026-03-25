package io.github.shomah4a.alle.script.graalpy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.styling.Face;
import io.github.shomah4a.alle.core.styling.RegexStyler;
import io.github.shomah4a.alle.core.styling.StyledSpan;
import io.github.shomah4a.alle.core.styling.SyntaxStyler;
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

    @Test
    void PythonからPatternCompileを呼べる() {
        context.eval("python", """
                import java
                Pattern = java.type('java.util.regex.Pattern')
                p = Pattern.compile('#.*$')
                """);
        Value result = context.eval("python", "p.pattern()");
        assertEquals("#.*$", result.asString());
    }

    @Test
    void PythonからFaceのstaticフィールドにアクセスできる() {
        context.eval("python", """
                import java
                Face = java.type('io.github.shomah4a.alle.core.styling.Face')
                kw = Face.KEYWORD
                """);
        Value result = context.eval("python", "kw");
        Face face = result.as(Face.class);
        assertEquals(Face.KEYWORD, face);
    }

    @Test
    void PythonからStylingRuleのレコード型をインスタンス化できる() {
        context.eval("python", """
                import java
                Pattern = java.type('java.util.regex.Pattern')
                Face = java.type('io.github.shomah4a.alle.core.styling.Face')
                PatternMatch = java.type('io.github.shomah4a.alle.core.styling.StylingRule$PatternMatch')
                rule = PatternMatch(Pattern.compile('#.*$'), Face.COMMENT)
                """);
        Value result = context.eval("python", "rule");
        assertTrue(result.isHostObject());
    }

    @Test
    void PythonからRegionMatchをインスタンス化できる() {
        context.eval("python", """
                import java
                Pattern = java.type('java.util.regex.Pattern')
                Face = java.type('io.github.shomah4a.alle.core.styling.Face')
                RegionMatch = java.type('io.github.shomah4a.alle.core.styling.StylingRule$RegionMatch')
                rule = RegionMatch(Pattern.compile('\"\"\"'), Pattern.compile('\"\"\"'), Face.STRING)
                """);
        Value result = context.eval("python", "rule");
        assertTrue(result.isHostObject());
    }

    @Test
    void PythonからEclipseCollectionsのListsを操作できる() {
        context.eval("python", """
                import java
                Lists = java.type('org.eclipse.collections.api.factory.Lists')
                lst = Lists.mutable.empty()
                lst.add("a")
                lst.add("b")
                """);
        Value result = context.eval("python", "lst.size()");
        assertEquals(2, result.asInt());
    }

    @Test
    void PythonからRegexStylerを生成してスタイリングできる() {
        context.eval("python", """
                import java
                Pattern = java.type('java.util.regex.Pattern')
                Face = java.type('io.github.shomah4a.alle.core.styling.Face')
                PatternMatch = java.type('io.github.shomah4a.alle.core.styling.StylingRule$PatternMatch')
                Lists = java.type('org.eclipse.collections.api.factory.Lists')
                RegexStyler = java.type('io.github.shomah4a.alle.core.styling.RegexStyler')

                rules = Lists.mutable.empty()
                rules.add(PatternMatch(Pattern.compile('#.*$'), Face.COMMENT))
                styler = RegexStyler(rules)
                """);
        Value pyStyler = context.eval("python", "styler");
        SyntaxStyler styler = pyStyler.as(SyntaxStyler.class);
        var spans = styler.styleLine("x = 1  # comment");
        assertFalse(spans.isEmpty());
        StyledSpan span = spans.get(0);
        assertEquals(Face.COMMENT, span.face());
    }

    @Test
    void PythonからMajorModeのstylerとしてRegexStylerを返せる() {
        context.eval("python", """
                import java
                from java.util import Optional
                Pattern = java.type('java.util.regex.Pattern')
                Face = java.type('io.github.shomah4a.alle.core.styling.Face')
                PatternMatch = java.type('io.github.shomah4a.alle.core.styling.StylingRule$PatternMatch')
                Lists = java.type('org.eclipse.collections.api.factory.Lists')
                RegexStyler = java.type('io.github.shomah4a.alle.core.styling.RegexStyler')
                MajorMode = java.type('io.github.shomah4a.alle.core.mode.MajorMode')

                rules = Lists.mutable.empty()
                rules.add(PatternMatch(Pattern.compile('#.*$'), Face.COMMENT))
                _styler = RegexStyler(rules)

                class TestPyMode(MajorMode):
                    def name(self):
                        return "TestPy"
                    def keymap(self):
                        return Optional.empty()
                    def styler(self):
                        return Optional.of(_styler)
                """);
        Value pyMode = context.eval("python", "TestPyMode()");
        MajorMode mode = pyMode.as(MajorMode.class);
        assertEquals("TestPy", mode.name());
        assertTrue(mode.styler().isPresent());
        SyntaxStyler styler = mode.styler().get();
        assertInstanceOf(RegexStyler.class, styler);
        var spans = styler.styleLine("# hello");
        assertFalse(spans.isEmpty());
        assertEquals(Face.COMMENT, spans.get(0).face());
    }
}
