package io.github.shomah4a.alle.core.mode.indent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.collections.api.factory.Sets;
import org.junit.jupiter.api.Test;

class CStyleIndentConfigTest {

    @Test
    void インデント幅が保持される() {
        var config = new CStyleIndentConfig(
                2,
                Sets.immutable.with('(', '[', '{'),
                Sets.immutable.with(')', ']', '}'),
                Sets.immutable.with("comment"));
        assertEquals(2, config.indentWidth());
    }

    @Test
    void 開き括弧の集合が保持される() {
        var config = new CStyleIndentConfig(
                2,
                Sets.immutable.with('(', '[', '{'),
                Sets.immutable.with(')', ']', '}'),
                Sets.immutable.with("comment"));
        assertTrue(config.openBrackets().contains('{'));
        assertTrue(config.openBrackets().contains('('));
        assertTrue(config.openBrackets().contains('['));
        assertFalse(config.openBrackets().contains('}'));
    }

    @Test
    void 閉じ括弧の集合が保持される() {
        var config = new CStyleIndentConfig(
                2,
                Sets.immutable.with('(', '[', '{'),
                Sets.immutable.with(')', ']', '}'),
                Sets.immutable.with("comment"));
        assertTrue(config.closeBrackets().contains('}'));
        assertTrue(config.closeBrackets().contains(')'));
        assertTrue(config.closeBrackets().contains(']'));
        assertFalse(config.closeBrackets().contains('{'));
    }

    @Test
    void JSON用の括弧セットで丸括弧が含まれない() {
        var config = new CStyleIndentConfig(
                2, Sets.immutable.with('[', '{'), Sets.immutable.with(']', '}'), Sets.immutable.with("comment"));
        assertTrue(config.openBrackets().contains('{'));
        assertTrue(config.openBrackets().contains('['));
        assertFalse(config.openBrackets().contains('('));
    }

    @Test
    void コメントノード型の集合が保持される() {
        var config = new CStyleIndentConfig(
                2,
                Sets.immutable.with('(', '[', '{'),
                Sets.immutable.with(')', ']', '}'),
                Sets.immutable.with("comment"));
        assertTrue(config.commentNodeTypes().contains("comment"));
    }

    @Test
    void Java用のコメントノード型集合には行コメントとブロックコメントの両方が含まれる() {
        var config = new CStyleIndentConfig(
                4,
                Sets.immutable.with('(', '[', '{'),
                Sets.immutable.with(')', ']', '}'),
                Sets.immutable.with("line_comment", "block_comment"));
        assertTrue(config.commentNodeTypes().contains("line_comment"));
        assertTrue(config.commentNodeTypes().contains("block_comment"));
        assertFalse(config.commentNodeTypes().contains("comment"));
    }
}
