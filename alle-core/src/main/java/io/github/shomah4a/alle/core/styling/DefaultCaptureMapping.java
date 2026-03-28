package io.github.shomah4a.alle.core.styling;

import org.eclipse.collections.api.factory.Maps;

/**
 * Tree-sitter公式のキャプチャ名慣習に基づくデフォルトのFaceNameマッピング。
 *
 * <p>公式の {@code highlights.scm} はドット区切りの階層的なキャプチャ名
 * （例: {@code @function.builtin}）を使用しており、この慣習は全言語で共通である。
 * 言語固有のキャプチャ名が追加される場合は、このマッピングを拡張して使用する。
 */
public final class DefaultCaptureMapping {

    private DefaultCaptureMapping() {}

    /**
     * 公式キャプチャ名→FaceNameのデフォルトマッピング。
     */
    public static final NodeFaceMapping INSTANCE = new NodeFaceMapping(Maps.mutable
            .<String, FaceName>empty()
            // コメント・文字列・数値
            .withKeyValue("comment", FaceName.COMMENT)
            .withKeyValue("string", FaceName.STRING)
            .withKeyValue("number", FaceName.NUMBER)
            .withKeyValue("escape", FaceName.STRING)
            // キーワード・演算子
            .withKeyValue("keyword", FaceName.KEYWORD)
            .withKeyValue("operator", FaceName.OPERATOR)
            // 関数
            .withKeyValue("function", FaceName.FUNCTION_NAME)
            .withKeyValue("function.method", FaceName.FUNCTION_NAME)
            .withKeyValue("function.builtin", FaceName.BUILTIN)
            // 型・定数・変数
            .withKeyValue("type", FaceName.TYPE)
            .withKeyValue("constructor", FaceName.TYPE)
            .withKeyValue("constant", FaceName.VARIABLE)
            .withKeyValue("constant.builtin", FaceName.BUILTIN)
            .withKeyValue("property", FaceName.VARIABLE)
            .withKeyValue("variable", FaceName.VARIABLE)
            // 句読点・埋め込み
            .withKeyValue("punctuation.special", FaceName.OPERATOR)
            .withKeyValue("embedded", FaceName.DEFAULT)
            .toImmutable());
}
