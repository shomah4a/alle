package io.github.shomah4a.alle.core.syntax;

import io.github.shomah4a.alle.core.styling.DefaultCaptureMapping;
import io.github.shomah4a.alle.core.styling.HighlightQueryLoader;
import io.github.shomah4a.alle.core.styling.SyntaxStyler;
import io.github.shomah4a.alle.core.styling.TreeSitterStyler;
import java.util.Optional;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.treesitter.TreeSitterBash;
import org.treesitter.TreeSitterJavascript;
import org.treesitter.TreeSitterJson;
import org.treesitter.TreeSitterPython;
import org.treesitter.TreeSitterYaml;

/**
 * 言語名から{@link SyntaxAnalyzer}と{@link SyntaxStyler}を生成するレジストリ。
 *
 * <p>同一言語のAnalyzerとStylerは共通の{@link TreeSitterSession}を共有し、
 * 同一テキストの2重パースを回避する。
 */
public class SyntaxAnalyzerRegistry {

    private final MutableMap<String, TreeSitterLanguageConfig> configs = UnifiedMap.newMap();

    /**
     * 言語名に対応するTree-sitter設定を登録する。
     *
     * @param language 言語名（例: "python"）
     * @param config 言語設定
     */
    public void register(String language, TreeSitterLanguageConfig config) {
        configs.put(language, config);
    }

    /**
     * 言語名に対応するセッション、アナライザー、スタイラーを生成して返す。
     * 同一セッションを共有するため、1回の呼び出しで両方を取得する。
     *
     * @param language 言語名
     * @return セッション・アナライザー・スタイラーの組（未登録の場合はempty）
     */
    public Optional<LanguageSupport> create(String language) {
        TreeSitterLanguageConfig config = configs.get(language);
        if (config == null) {
            return Optional.empty();
        }
        var session = new TreeSitterSession(config.language());
        var analyzer = new TreeSitterAnalyzer(session, config.bracketTypes());
        var styler = new TreeSitterStyler(session, config.queryString(), config.captureMapping());
        return Optional.of(new LanguageSupport(analyzer, styler));
    }

    /**
     * アナライザーとスタイラーの組。
     */
    public record LanguageSupport(SyntaxAnalyzer analyzer, SyntaxStyler styler) {}

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

    /** JavaScript用の括弧系ノードタイプ名。 */
    private static final ImmutableSet<String> JAVASCRIPT_BRACKET_TYPES = Sets.immutable.with(
            "parenthesized_expression",
            "arguments",
            "formal_parameters",
            "array",
            "object",
            "template_string",
            "subscript_expression",
            "statement_block",
            "class_body",
            "switch_body");

    /** JSON用の括弧系ノードタイプ名。 */
    private static final ImmutableSet<String> JSON_BRACKET_TYPES = Sets.immutable.with("object", "array");

    /** YAML用の括弧系ノードタイプ名。フロースタイルの括弧のみ対象とする。 */
    private static final ImmutableSet<String> YAML_BRACKET_TYPES = Sets.immutable.with("flow_mapping", "flow_sequence");

    /** Bash用の括弧系ノードタイプ名。 */
    private static final ImmutableSet<String> BASH_BRACKET_TYPES =
            Sets.immutable.with("compound_statement", "subshell", "command_substitution");

    /**
     * 組み込み言語を登録済みのレジストリを生成する。
     *
     * @return 組み込み言語が登録されたレジストリ
     */
    public static SyntaxAnalyzerRegistry createWithBuiltins() {
        var registry = new SyntaxAnalyzerRegistry();
        String pythonQuery = HighlightQueryLoader.load("python");
        registry.register(
                "python",
                new TreeSitterLanguageConfig(
                        new TreeSitterPython(), pythonQuery, DefaultCaptureMapping.INSTANCE, PYTHON_BRACKET_TYPES));
        String javascriptQuery = HighlightQueryLoader.load("javascript");
        registry.register(
                "javascript",
                new TreeSitterLanguageConfig(
                        new TreeSitterJavascript(),
                        javascriptQuery,
                        DefaultCaptureMapping.INSTANCE,
                        JAVASCRIPT_BRACKET_TYPES));
        String jsonQuery = HighlightQueryLoader.load("json");
        registry.register(
                "json",
                new TreeSitterLanguageConfig(
                        new TreeSitterJson(), jsonQuery, DefaultCaptureMapping.INSTANCE, JSON_BRACKET_TYPES));
        String yamlQuery = HighlightQueryLoader.load("yaml");
        registry.register(
                "yaml",
                new TreeSitterLanguageConfig(
                        new TreeSitterYaml(), yamlQuery, DefaultCaptureMapping.INSTANCE, YAML_BRACKET_TYPES));
        String bashQuery = HighlightQueryLoader.load("bash");
        registry.register(
                "bash",
                new TreeSitterLanguageConfig(
                        new TreeSitterBash(), bashQuery, DefaultCaptureMapping.INSTANCE, BASH_BRACKET_TYPES));
        return registry;
    }
}
