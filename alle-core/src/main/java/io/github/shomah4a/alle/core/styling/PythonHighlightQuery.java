package io.github.shomah4a.alle.core.styling;

import org.eclipse.collections.api.factory.Maps;

/**
 * Python言語用のTree-sitterハイライトクエリとキャプチャ名マッピングを提供する。
 */
public final class PythonHighlightQuery {

    private PythonHighlightQuery() {}

    /**
     * Python用のS式クエリ文字列。
     * キャプチャ名はNodeFaceMappingでFaceNameに変換される。
     */
    public static final String QUERY = """
            ; コメント
            (comment) @comment

            ; 文字列
            (string) @string

            ; 数値
            (integer) @number
            (float) @number

            ; 組み込み定数
            (true) @builtin
            (false) @builtin
            (none) @builtin

            ; キーワード
            [
              "and" "as" "assert" "async" "await" "break" "class" "continue"
              "def" "del" "elif" "else" "except" "finally" "for" "from"
              "global" "if" "import" "in" "is" "lambda" "nonlocal" "not"
              "or" "pass" "raise" "return" "try" "while" "with" "yield"
              "match" "case"
            ] @keyword

            ; 演算子
            [
              "+" "-" "*" "/" "%" "**" "//" "|" "&" "^" "~" "<<" ">>"
              "=" "+=" "-=" "*=" "/=" "%=" "**=" "//=" "|=" "&=" "^=" "<<=" ">>="
              "==" "!=" "<" ">" "<=" ">="
              "->"
            ] @operator

            ; デコレータ
            (decorator) @annotation

            ; 関数定義の関数名
            (function_definition name: (identifier) @function_name)

            ; クラス定義のクラス名
            (class_definition name: (identifier) @type)

            ; 関数呼び出しの関数名
            (call function: (identifier) @function_name)
            (call function: (attribute attribute: (identifier) @function_name))

            ; 型注釈
            (type (identifier) @type)

            ; self / cls パラメータ
            ((identifier) @builtin
              (#match? @builtin "^(self|cls)$"))

            ; 識別子（他のキャプチャに該当しないもの）
            (identifier) @variable
            """;

    /**
     * キャプチャ名→FaceNameのマッピング。
     */
    public static final NodeFaceMapping MAPPING = new NodeFaceMapping(Maps.mutable
            .<String, FaceName>empty()
            .withKeyValue("comment", FaceName.COMMENT)
            .withKeyValue("string", FaceName.STRING)
            .withKeyValue("number", FaceName.NUMBER)
            .withKeyValue("builtin", FaceName.BUILTIN)
            .withKeyValue("keyword", FaceName.KEYWORD)
            .withKeyValue("operator", FaceName.OPERATOR)
            .withKeyValue("annotation", FaceName.ANNOTATION)
            .withKeyValue("function_name", FaceName.FUNCTION_NAME)
            .withKeyValue("type", FaceName.TYPE)
            .withKeyValue("variable", FaceName.VARIABLE)
            .toImmutable());
}
