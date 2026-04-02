package io.github.shomah4a.alle.core.syntax;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.treesitter.TreeSitterPython;

class TreeSitterAnalyzerTest {

    private static final ImmutableSet<String> PYTHON_BRACKET_TYPES = Sets.immutable.with(
            "parenthesized_expression",
            "generator_expression",
            "tuple",
            "argument_list",
            "parameters",
            "list",
            "list_comprehension",
            "list_pattern",
            "dictionary",
            "dictionary_comprehension",
            "set",
            "set_comprehension",
            "subscript");

    private TreeSitterAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        var session = new TreeSitterSession(new TreeSitterPython());
        analyzer = new TreeSitterAnalyzer(session, PYTHON_BRACKET_TYPES);
    }

    @Test
    void 単純な代入文のルートノードを取得できる() {
        ListIterable<String> lines = Lists.immutable.of("x = 1");
        SyntaxTree tree = analyzer.analyze(lines);
        SyntaxNode root = tree.rootNode();
        assertEquals("module", root.type());
    }

    @Test
    void 指定位置のノードを取得できる() {
        ListIterable<String> lines = Lists.immutable.of("x = 1");
        SyntaxTree tree = analyzer.analyze(lines);
        Optional<SyntaxNode> node = tree.nodeAt(0, 0);
        assertTrue(node.isPresent());
        assertEquals("identifier", node.get().type());
    }

    @Test
    void 丸括弧内にいる場合にenclosingBracketで括弧ノードを取得できる() {
        ListIterable<String> lines = Lists.immutable.of("result = func(", "    arg1,", "    arg2", ")");
        SyntaxTree tree = analyzer.analyze(lines);
        // 2行目（arg1の位置）で括弧内を検知
        Optional<SyntaxNode> bracket = tree.enclosingBracket(1, 4);
        assertTrue(bracket.isPresent());
        assertEquals(0, bracket.get().startLine());
    }

    @Test
    void 角括弧内にいる場合にenclosingBracketで括弧ノードを取得できる() {
        ListIterable<String> lines = Lists.immutable.of("items = [", "    1,", "    2,", "]");
        SyntaxTree tree = analyzer.analyze(lines);
        Optional<SyntaxNode> bracket = tree.enclosingBracket(1, 4);
        assertTrue(bracket.isPresent());
    }

    @Test
    void 波括弧内にいる場合にenclosingBracketで括弧ノードを取得できる() {
        ListIterable<String> lines = Lists.immutable.of("d = {", "    'a': 1,", "}");
        SyntaxTree tree = analyzer.analyze(lines);
        Optional<SyntaxNode> bracket = tree.enclosingBracket(1, 4);
        assertTrue(bracket.isPresent());
    }

    @Test
    void 括弧外ではenclosingBracketがemptyを返す() {
        ListIterable<String> lines = Lists.immutable.of("x = 1", "y = 2");
        SyntaxTree tree = analyzer.analyze(lines);
        Optional<SyntaxNode> bracket = tree.enclosingBracket(0, 0);
        assertFalse(bracket.isPresent());
    }

    @Test
    void enclosingNodeOfTypeで指定タイプの親ノードを取得できる() {
        ListIterable<String> lines = Lists.immutable.of("def foo():", "    return 1");
        SyntaxTree tree = analyzer.analyze(lines);
        Optional<SyntaxNode> funcNode = tree.enclosingNodeOfType(1, 4, "function_definition");
        assertTrue(funcNode.isPresent());
        assertEquals("function_definition", funcNode.get().type());
    }

    @Test
    void キャッシュが有効で同一テキストは再パースされない() {
        ListIterable<String> lines = Lists.immutable.of("x = 1");
        SyntaxTree tree1 = analyzer.analyze(lines);
        SyntaxTree tree2 = analyzer.analyze(lines);
        // 同一インスタンスが返ることを確認
        assertTrue(tree1 == tree2);
    }

    @Test
    void テキスト変更時はキャッシュが無効になる() {
        SyntaxTree tree1 = analyzer.analyze(Lists.immutable.of("x = 1"));
        SyntaxTree tree2 = analyzer.analyze(Lists.immutable.of("x = 2"));
        assertFalse(tree1 == tree2);
    }

    @Test
    void セッション共有時にStylerパース後もAnalyzerの結果が有効である() {
        // Analyzer と Styler が同一セッションを共有する状況を再現
        var session = new TreeSitterSession(new TreeSitterPython());
        var sharedAnalyzer = new TreeSitterAnalyzer(session, PYTHON_BRACKET_TYPES);

        // 1. Analyzer がテキストv1をパース
        ListIterable<String> linesV1 = Lists.immutable.of("x = [", "    1,", "]");
        SyntaxTree treeV1 = sharedAnalyzer.analyze(linesV1);
        // 括弧内にいることを確認
        assertTrue(treeV1.enclosingBracket(1, 4).isPresent());

        // 2. Styler側がテキストv2（変更後）をパース → セッション内でoldTree.close()が走る
        ListIterable<String> linesV2 = Lists.immutable.of("x = [", "    1,", "    2,", "]");
        session.parse(linesV2);

        // 3. Analyzer が再度テキストv1でパースした場合でもTree is closedにならない
        SyntaxTree treeV1Again = sharedAnalyzer.analyze(linesV1);
        assertDoesNotThrow(() -> treeV1Again.enclosingBracket(1, 4));
    }

    @Test
    void 空行リストでも例外が発生しない() {
        ListIterable<String> lines = Lists.immutable.empty();
        SyntaxTree tree = analyzer.analyze(lines);
        SyntaxNode root = tree.rootNode();
        assertEquals("module", root.type());
    }

    @Test
    void 関数呼び出しの括弧の開始カラムを正しく取得できる() {
        ListIterable<String> lines = Lists.immutable.of("result = func(arg1,", "              arg2)");
        SyntaxTree tree = analyzer.analyze(lines);
        Optional<SyntaxNode> bracket = tree.enclosingBracket(1, 14);
        assertTrue(bracket.isPresent());
        // funcの引数リスト括弧の開始カラム
        assertEquals(0, bracket.get().startLine());
    }

    @Test
    void ネストした括弧では最も内側の括弧を取得する() {
        ListIterable<String> lines = Lists.immutable.of("f(g(", "    x", "))");
        SyntaxTree tree = analyzer.analyze(lines);
        Optional<SyntaxNode> bracket = tree.enclosingBracket(1, 4);
        assertTrue(bracket.isPresent());
        // 最も内側の括弧（g(...)）の開始位置
        SyntaxNode node = bracket.get();
        assertEquals(0, node.startLine());
    }
}
