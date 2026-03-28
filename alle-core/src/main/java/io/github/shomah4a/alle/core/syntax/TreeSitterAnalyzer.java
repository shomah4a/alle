package io.github.shomah4a.alle.core.syntax;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.jspecify.annotations.Nullable;
import org.treesitter.TSLanguage;
import org.treesitter.TSParser;
import org.treesitter.TSTree;

/**
 * Tree-sitterによる{@link SyntaxAnalyzer}実装。
 *
 * <p>前回のパース結果をキャッシュし、テキストが変更されていない場合はキャッシュを返す。
 * テキストが変更された場合はフルパースを実行する。
 *
 * <p>インスタンスはバッファごとのモードが保持するため、ステートのスコープはバッファに閉じている。
 */
public class TreeSitterAnalyzer implements SyntaxAnalyzer {

    private final TSLanguage language;
    private final ImmutableSet<String> bracketTypes;
    private @Nullable String cachedText;
    private @Nullable TSTree cachedTree;
    private @Nullable TreeSitterSyntaxTree cachedResult;

    /**
     * @param language Tree-sitterの言語定義
     * @param bracketTypes 括弧系ノードとみなすノードタイプ名の集合
     */
    public TreeSitterAnalyzer(TSLanguage language, ImmutableSet<String> bracketTypes) {
        this.language = language;
        this.bracketTypes = bracketTypes;
    }

    @Override
    public SyntaxTree analyze(ListIterable<String> lines) {
        if (lines.isEmpty()) {
            clearCache();
            return new TreeSitterSyntaxTree(parse(""), lines, bracketTypes);
        }

        String fullText = lines.makeString("\n");

        if (fullText.equals(cachedText) && cachedResult != null) {
            return cachedResult;
        }

        TSTree newTree = parse(fullText);

        if (cachedTree != null) {
            cachedTree.close();
        }

        cachedTree = newTree;
        cachedText = fullText;
        cachedResult = new TreeSitterSyntaxTree(newTree, lines, bracketTypes);
        return cachedResult;
    }

    private TSTree parse(String fullText) {
        try (TSParser parser = new TSParser()) {
            parser.setLanguage(language);
            return parser.parseString(null, fullText);
        }
    }

    private void clearCache() {
        if (cachedTree != null) {
            cachedTree.close();
            cachedTree = null;
        }
        cachedText = null;
        cachedResult = null;
    }
}
