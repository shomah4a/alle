package io.github.shomah4a.alle.core.mode.indent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.collections.api.factory.Sets;
import org.junit.jupiter.api.Test;

class CStyleIndentConfigTest {

    @Test
    void インデント幅が保持される() {
        var config = new CStyleIndentConfig(2, Sets.immutable.with('(', '[', '{'), Sets.immutable.with(')', ']', '}'));
        assertEquals(2, config.indentWidth());
    }

    @Test
    void 開き括弧の集合が保持される() {
        var config = new CStyleIndentConfig(2, Sets.immutable.with('(', '[', '{'), Sets.immutable.with(')', ']', '}'));
        assertTrue(config.openBrackets().contains('{'));
        assertTrue(config.openBrackets().contains('('));
        assertTrue(config.openBrackets().contains('['));
        assertFalse(config.openBrackets().contains('}'));
    }

    @Test
    void 閉じ括弧の集合が保持される() {
        var config = new CStyleIndentConfig(2, Sets.immutable.with('(', '[', '{'), Sets.immutable.with(')', ']', '}'));
        assertTrue(config.closeBrackets().contains('}'));
        assertTrue(config.closeBrackets().contains(')'));
        assertTrue(config.closeBrackets().contains(']'));
        assertFalse(config.closeBrackets().contains('{'));
    }

    @Test
    void JSON用の括弧セットで丸括弧が含まれない() {
        var config = new CStyleIndentConfig(2, Sets.immutable.with('[', '{'), Sets.immutable.with(']', '}'));
        assertTrue(config.openBrackets().contains('{'));
        assertTrue(config.openBrackets().contains('['));
        assertFalse(config.openBrackets().contains('('));
    }
}
