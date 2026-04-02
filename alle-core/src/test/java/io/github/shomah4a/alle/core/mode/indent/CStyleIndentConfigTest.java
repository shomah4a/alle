package io.github.shomah4a.alle.core.mode.indent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.collections.api.factory.Sets;
import org.junit.jupiter.api.Test;

class CStyleIndentConfigTest {

    @Test
    void 角括弧を含む開き括弧でパターンが正しく生成される() {
        var config = assertDoesNotThrow(
                () -> CStyleIndentConfig.of(2, Sets.immutable.with('(', '[', '{'), Sets.immutable.with(')', ']', '}')));
        assertTrue(config.openBracketEndPattern().matcher("{").find());
        assertTrue(config.openBracketEndPattern().matcher("(").find());
        assertTrue(config.openBracketEndPattern().matcher("[").find());
    }

    @Test
    void 閉じ括弧パターンが行頭の閉じ括弧にマッチする() {
        var config = CStyleIndentConfig.of(2, Sets.immutable.with('(', '[', '{'), Sets.immutable.with(')', ']', '}'));
        assertTrue(config.closeBracketStartPattern().matcher("}").find());
        assertTrue(config.closeBracketStartPattern().matcher("  }").find());
        assertTrue(config.closeBracketStartPattern().matcher("  ]").find());
        assertTrue(config.closeBracketStartPattern().matcher(")").find());
    }

    @Test
    void 開き括弧パターンがCスタイルコメント付き行末にマッチする() {
        var config = CStyleIndentConfig.of(2, Sets.immutable.with('(', '[', '{'), Sets.immutable.with(')', ']', '}'));
        assertTrue(config.openBracketEndPattern().matcher("function() {").find());
        assertTrue(config.openBracketEndPattern().matcher("{ // comment").find());
        assertTrue(config.openBracketEndPattern().matcher("{ /* comment").find());
    }

    @Test
    void 開き括弧パターンが通常テキストにはマッチしない() {
        var config = CStyleIndentConfig.of(2, Sets.immutable.with('(', '[', '{'), Sets.immutable.with(')', ']', '}'));
        assertFalse(config.openBracketEndPattern().matcher("hello").find());
        assertFalse(config.openBracketEndPattern().matcher("x = 1;").find());
    }

    @Test
    void JSON用の括弧セットで正しく生成される() {
        var config = assertDoesNotThrow(
                () -> CStyleIndentConfig.of(2, Sets.immutable.with('[', '{'), Sets.immutable.with(']', '}')));
        assertTrue(config.openBracketEndPattern().matcher("{").find());
        assertTrue(config.openBracketEndPattern().matcher("[").find());
        assertFalse(config.openBracketEndPattern().matcher("(").find());
    }
}
