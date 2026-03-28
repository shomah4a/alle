package io.github.shomah4a.alle.core.syntax;

import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.treesitter.TreeSitterPython;

/**
 * 言語名から{@link SyntaxAnalyzer}を生成するレジストリ。
 *
 * <p>スクリプト側からは言語名を指定してアナライザーを取得する。
 * 内部でTree-sitterを使用しているかどうかはスクリプト側には見えない。
 */
public class SyntaxAnalyzerRegistry {

    private final MutableMap<String, Supplier<SyntaxAnalyzer>> factories = UnifiedMap.newMap();

    /**
     * 言語名に対応するアナライザーファクトリを登録する。
     *
     * @param language 言語名（例: "python"）
     * @param factory アナライザーのファクトリ
     */
    public void register(String language, Supplier<SyntaxAnalyzer> factory) {
        factories.put(language, factory);
    }

    /**
     * 言語名に対応するアナライザーを生成して返す。
     *
     * @param language 言語名
     * @return アナライザー（未登録の場合はempty）
     */
    public Optional<SyntaxAnalyzer> create(String language) {
        Supplier<SyntaxAnalyzer> factory = factories.get(language);
        if (factory == null) {
            return Optional.empty();
        }
        return Optional.of(factory.get());
    }

    /**
     * 組み込み言語を登録済みのレジストリを生成する。
     *
     * @return 組み込み言語が登録されたレジストリ
     */
    /** Python用の括弧系ノードタイプ名。 */
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

    /**
     * 組み込み言語を登録済みのレジストリを生成する。
     *
     * @return 組み込み言語が登録されたレジストリ
     */
    public static SyntaxAnalyzerRegistry createWithBuiltins() {
        var registry = new SyntaxAnalyzerRegistry();
        registry.register("python", () -> new TreeSitterAnalyzer(new TreeSitterPython(), PYTHON_BRACKET_TYPES));
        return registry;
    }
}
