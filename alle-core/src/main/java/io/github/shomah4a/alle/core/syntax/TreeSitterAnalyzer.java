package io.github.shomah4a.alle.core.syntax;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.jspecify.annotations.Nullable;
import org.treesitter.TSTree;

/**
 * Tree-sitterによる{@link SyntaxAnalyzer}実装。
 *
 * <p>{@link TreeSitterSession}にパースを委譲し、結果をSyntaxTreeとして提供する。
 *
 * <p>インスタンスはバッファごとのモードが保持するため、ステートのスコープはバッファに閉じている。
 */
public class TreeSitterAnalyzer implements SyntaxAnalyzer {

    private final TreeSitterSession session;
    private final ImmutableSet<String> bracketTypes;
    private @Nullable String cachedText;
    private @Nullable TreeSitterSyntaxTree cachedResult;

    /**
     * @param session パースを管理するセッション
     * @param bracketTypes 括弧系ノードとみなすノードタイプ名の集合
     */
    public TreeSitterAnalyzer(TreeSitterSession session, ImmutableSet<String> bracketTypes) {
        this.session = session;
        this.bracketTypes = bracketTypes;
    }

    @Override
    public SyntaxTree analyze(ListIterable<String> lines) {
        TSTree tree = session.parse(lines);
        String fullText = lines.isEmpty() ? "" : lines.makeString("\n");

        if (fullText.equals(cachedText) && cachedResult != null) {
            return cachedResult;
        }

        cachedText = fullText;
        cachedResult = new TreeSitterSyntaxTree(tree, lines, bracketTypes);
        return cachedResult;
    }
}
