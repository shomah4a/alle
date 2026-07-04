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
import org.treesitter.TreeSitterHcl;
import org.treesitter.TreeSitterJava;
import org.treesitter.TreeSitterJavascript;
import org.treesitter.TreeSitterJson;
import org.treesitter.TreeSitterPython;
import org.treesitter.TreeSitterTypescript;
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

    /**
     * TypeScript 用の括弧系ノードタイプ名。
     * JavaScript の括弧タイプに加え、TypeScript 固有のブロック構造 ({@code interface_body} /
     * {@code enum_body} / {@code object_type} / {@code tuple_type}) を追加する。
     * {@code type_parameters} / {@code type_arguments} ({@code <>}) は
     * CStyleIndentConfig が文字ベース ({@code (}, {@code [}, 波括弧) 判定のため対象外（ADR 0137 参照）。
     */
    private static final ImmutableSet<String> TYPESCRIPT_BRACKET_TYPES = Sets.immutable.with(
            "parenthesized_expression",
            "arguments",
            "formal_parameters",
            "array",
            "object",
            "template_string",
            "subscript_expression",
            "statement_block",
            "class_body",
            "switch_body",
            "interface_body",
            "enum_body",
            "object_type",
            "tuple_type");

    /**
     * Java 用の括弧系ノードタイプ名。
     * {@code type_parameters} / {@code type_arguments}（{@code <T, U>}）は
     * CStyleIndentConfig が文字ベース（{@code (}, {@code [}, 波括弧）判定のため対象外（ADR 0142 参照）。
     * ここに列挙する全ノード型は tree-sitter-java v0.23.4 の node-types.json に存在することを確認済み。
     */
    private static final ImmutableSet<String> JAVA_BRACKET_TYPES = Sets.immutable.with(
            "parenthesized_expression",
            "argument_list",
            "formal_parameters",
            "inferred_parameters",
            "annotation_argument_list",
            "resource_specification",
            "array_initializer",
            "array_access",
            "element_value_array_initializer",
            "block",
            "constructor_body",
            "class_body",
            "interface_body",
            "enum_body",
            "annotation_type_body",
            "switch_block",
            "module_body");

    /** JSON用の括弧系ノードタイプ名。 */
    private static final ImmutableSet<String> JSON_BRACKET_TYPES = Sets.immutable.with("object", "array");

    /** YAML用の括弧系ノードタイプ名。フロースタイルの括弧のみ対象とする。 */
    private static final ImmutableSet<String> YAML_BRACKET_TYPES = Sets.immutable.with("flow_mapping", "flow_sequence");

    /** Bash用の括弧系ノードタイプ名。 */
    private static final ImmutableSet<String> BASH_BRACKET_TYPES =
            Sets.immutable.with("compound_statement", "subshell", "command_substitution");

    /**
     * HCL用の括弧系ノードタイプ名。
     * HCL AST は {@code block_start}/{@code object_start}/{@code tuple_start} 等のラッパーで {@code {}}/{@code []} を包む構造のため、
     * {@code CStyleIndentState.findFirstContentChild} の前提と整合しない。
     * {@code enclosingBracket} 経由のインデント解決は使わず、{@code isOpenBracketBeforeColumn} の文字ベース判定のみに頼る（ADR 0136参照）。
     */
    private static final ImmutableSet<String> HCL_BRACKET_TYPES = Sets.immutable.empty();

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
        String hclQuery = HighlightQueryLoader.load("hcl");
        registry.register(
                "hcl",
                new TreeSitterLanguageConfig(
                        new TreeSitterHcl(), hclQuery, DefaultCaptureMapping.INSTANCE, HCL_BRACKET_TYPES));
        String typescriptQuery = HighlightQueryLoader.load("typescript");
        registry.register(
                "typescript",
                new TreeSitterLanguageConfig(
                        new TreeSitterTypescript(),
                        typescriptQuery,
                        DefaultCaptureMapping.INSTANCE,
                        TYPESCRIPT_BRACKET_TYPES));
        String javaQuery = HighlightQueryLoader.load("java");
        registry.register(
                "java",
                new TreeSitterLanguageConfig(
                        new TreeSitterJava(), javaQuery, DefaultCaptureMapping.INSTANCE, JAVA_BRACKET_TYPES));
        return registry;
    }
}
