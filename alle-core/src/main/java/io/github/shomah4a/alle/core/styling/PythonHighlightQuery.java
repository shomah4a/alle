package io.github.shomah4a.alle.core.styling;

import org.eclipse.collections.api.factory.Maps;

/**
 * Python言語用のTree-sitterハイライトクエリとキャプチャ名マッピングを提供する。
 *
 * <p>クエリ文字列は公式の {@code highlights.scm} をリソースから読み込む。
 * キャプチャ名→FaceNameのマッピングは公式のキャプチャ名慣習に従う。
 */
public final class PythonHighlightQuery {

    private PythonHighlightQuery() {}

    /**
     * 公式 highlights.scm のキャプチャ名→FaceNameのマッピング。
     *
     * <p>公式のキャプチャ名はドット区切り（例: {@code function.builtin}）で、
     * 階層的な分類になっている。より具体的なキャプチャ名が優先されるが、
     * マッピング上は各名前を個別に定義する。
     */
    public static final NodeFaceMapping MAPPING = new NodeFaceMapping(Maps.mutable
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
            // 句読点・埋め込み（控えめなスタイル）
            .withKeyValue("punctuation.special", FaceName.OPERATOR)
            .withKeyValue("embedded", FaceName.DEFAULT)
            .toImmutable());
}
