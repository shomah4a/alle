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
    private @Nullable TSTree cachedTree;
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

        // session.parse()が返したTSTreeインスタンスが前回と同一ならキャッシュを再利用する。
        // テキスト比較ではなくインスタンス比較にすることで、Styler側のパースにより
        // 旧TSTreeがcloseされた後にcachedResultを誤って返す問題を防ぐ。
        if (tree == cachedTree && cachedResult != null) {
            return cachedResult;
        }

        cachedTree = tree;
        cachedResult = new TreeSitterSyntaxTree(tree, lines, bracketTypes);
        return cachedResult;
    }
}
