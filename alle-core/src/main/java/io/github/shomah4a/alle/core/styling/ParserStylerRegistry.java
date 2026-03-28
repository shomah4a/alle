package io.github.shomah4a.alle.core.styling;

import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.treesitter.TreeSitterPython;

/**
 * 言語名からパーサーベースのSyntaxStylerを生成するレジストリ。
 *
 * <p>スクリプト側からは言語名を指定してスタイラーを取得する。
 * 内部でTree-sitterを使用しているかどうかはスクリプト側には見えない。
 */
public class ParserStylerRegistry {

    private final MutableMap<String, Supplier<SyntaxStyler>> factories = UnifiedMap.newMap();

    /**
     * 言語名に対応するスタイラーファクトリを登録する。
     *
     * @param language 言語名（例: "python"）
     * @param factory スタイラーのファクトリ
     */
    public void register(String language, Supplier<SyntaxStyler> factory) {
        factories.put(language, factory);
    }

    /**
     * 言語名に対応するスタイラーを生成して返す。
     *
     * @param language 言語名
     * @return スタイラー（未登録の場合はempty）
     */
    public Optional<SyntaxStyler> create(String language) {
        Supplier<SyntaxStyler> factory = factories.get(language);
        if (factory == null) {
            return Optional.empty();
        }
        return Optional.of(factory.get());
    }

    /**
     * 指定した言語が登録されているかを返す。
     *
     * @param language 言語名
     * @return 登録されている場合true
     */
    public boolean isSupported(String language) {
        return factories.containsKey(language);
    }

    /**
     * 組み込み言語を登録済みのレジストリを生成する。
     *
     * @return 組み込み言語が登録されたレジストリ
     */
    public static ParserStylerRegistry createWithBuiltins() {
        var registry = new ParserStylerRegistry();
        String pythonQuery = HighlightQueryLoader.load("python");
        registry.register(
                "python",
                () -> new TreeSitterStyler(new TreeSitterPython(), pythonQuery, PythonHighlightQuery.MAPPING));
        return registry;
    }
}
