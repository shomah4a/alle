package io.github.shomah4a.allei.core.io;

/**
 * 改行コードの種別。
 */
public enum LineEnding {
    LF("\n"),
    CRLF("\r\n"),
    CR("\r");

    private final String value;

    LineEnding(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * テキスト中の最初の改行コードから種別を検出する。
     * 改行が含まれない場合はLFを返す。
     */
    public static LineEnding detect(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    return CRLF;
                }
                return CR;
            }
            if (c == '\n') {
                return LF;
            }
        }
        return LF;
    }

    /**
     * テキスト中の改行コードをLFに正規化する。
     */
    public static String normalize(String text) {
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }

    /**
     * LFで正規化されたテキストを、指定の改行コードに変換する。
     */
    public String denormalize(String text) {
        if (this == LF) {
            return text;
        }
        return text.replace("\n", value);
    }
}
