package io.github.shomah4a.alle.core.styling;

/**
 * テキスト範囲の構文上の役割を表すセマンティック名。
 * 見た目の情報を一切持たない。具体的な視覚属性への解決は {@link FaceTheme} が行う。
 *
 * <p>組み込みの名前は定数として提供するが、スクリプトやプラグインから
 * 新しいセマンティック名を自由に追加できる。
 *
 * <p>等価性は {@code name} のみで判定する。
 */
public final class FaceName {

    private final String name;
    private final String description;

    public FaceName(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FaceName other)) {
            return false;
        }
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "FaceName[" + name + "]";
    }

    // 構文要素
    public static final FaceName DEFAULT = new FaceName("default", "テキストのデフォルトスタイル");
    public static final FaceName HEADING = new FaceName("heading", "見出し");
    public static final FaceName CODE = new FaceName("code", "コードブロック・インラインコード");
    public static final FaceName LINK = new FaceName("link", "ハイパーリンク・参照リンク");
    public static final FaceName STRING = new FaceName("string", "文字列リテラル");
    public static final FaceName COMMENT = new FaceName("comment", "コメント");
    public static final FaceName KEYWORD = new FaceName("keyword", "プログラミング言語のキーワード");
    public static final FaceName TABLE = new FaceName("table", "テーブル要素");
    public static final FaceName NUMBER = new FaceName("number", "数値リテラル");
    public static final FaceName ANNOTATION = new FaceName("annotation", "アノテーション・デコレータ");
    public static final FaceName LIST_MARKER = new FaceName("list-marker", "リストマーカー");
    public static final FaceName TYPE = new FaceName("type", "型名");
    public static final FaceName FUNCTION_NAME = new FaceName("function-name", "関数・メソッド名");
    public static final FaceName VARIABLE = new FaceName("variable", "変数名");
    public static final FaceName OPERATOR = new FaceName("operator", "演算子");
    public static final FaceName BUILTIN = new FaceName("builtin", "組み込み関数・定数");

    // インライン修飾（セマンティクス）
    public static final FaceName STRONG = new FaceName("strong", "強調テキスト（Markdownの**太字**等）");
    public static final FaceName EMPHASIS = new FaceName("emphasis", "強勢テキスト（Markdownの*斜体*等）");
    public static final FaceName DELETION = new FaceName("deletion", "削除テキスト（Markdownの~~取り消し線~~等）");

    // UI要素
    public static final FaceName MINIBUFFER_PROMPT = new FaceName("minibuffer-prompt", "ミニバッファのプロンプト文字列");

    // Dired
    public static final FaceName DIRED_HEADER = new FaceName("dired-header", "Diredのヘッダ行（ルートディレクトリパス）");
    public static final FaceName DIRED_DIRECTORY = new FaceName("dired-directory", "Diredのディレクトリ名");
}
