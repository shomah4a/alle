package io.github.shomah4a.alle.core.search;

import java.util.Optional;

/**
 * バッファ内テキスト検索エンジン。
 * buffer.getText()で取得した文字列に対してString.indexOf/lastIndexOfで検索する。
 * char offsetとcodepoint offsetの変換を内部で行う。
 */
public class BufferSearcher {

    private BufferSearcher() {}

    /**
     * 指定位置から前方検索する。
     * マッチが見つからない場合はバッファ先頭からラップアラウンドする。
     *
     * @param text バッファ全体のテキスト
     * @param query 検索クエリ
     * @param fromCodePointOffset 検索開始位置（コードポイント単位）
     * @return 検索結果。クエリが空またはマッチなしの場合はempty
     */
    public static Optional<SearchResult> searchForward(String text, String query, int fromCodePointOffset) {
        if (query.isEmpty()) {
            return Optional.empty();
        }

        int fromCharOffset = codePointOffsetToCharOffset(text, fromCodePointOffset);
        int charIndex = text.indexOf(query, fromCharOffset);

        if (charIndex >= 0) {
            return Optional.of(toSearchResult(text, query, charIndex, false));
        }

        // ラップアラウンド: 先頭から検索開始位置までを検索
        charIndex = text.indexOf(query, 0);
        if (charIndex >= 0 && charIndex < fromCharOffset) {
            return Optional.of(toSearchResult(text, query, charIndex, true));
        }

        return Optional.empty();
    }

    /**
     * 指定位置から後方検索する。
     * マッチが見つからない場合はバッファ末尾からラップアラウンドする。
     *
     * @param text バッファ全体のテキスト
     * @param query 検索クエリ
     * @param fromCodePointOffset 検索開始位置（コードポイント単位）
     * @return 検索結果。クエリが空またはマッチなしの場合はempty
     */
    public static Optional<SearchResult> searchBackward(String text, String query, int fromCodePointOffset) {
        if (query.isEmpty()) {
            return Optional.empty();
        }

        int fromCharOffset = codePointOffsetToCharOffset(text, fromCodePointOffset);
        // fromCharOffsetより前で最後に見つかるマッチを探す
        int charIndex = text.lastIndexOf(query, fromCharOffset - 1);

        if (charIndex >= 0) {
            return Optional.of(toSearchResult(text, query, charIndex, false));
        }

        // ラップアラウンド: バッファ末尾から検索
        charIndex = text.lastIndexOf(query);
        if (charIndex >= 0 && charIndex >= fromCharOffset) {
            return Optional.of(toSearchResult(text, query, charIndex, true));
        }

        return Optional.empty();
    }

    private static SearchResult toSearchResult(String text, String query, int charIndex, boolean wrapped) {
        int startCodePoint = charOffsetToCodePointOffset(text, charIndex);
        int queryCodePointLength = (int) query.codePoints().count();
        return new SearchResult(startCodePoint, startCodePoint + queryCodePointLength, wrapped);
    }

    /**
     * コードポイントオフセットをcharオフセットに変換する。
     */
    static int codePointOffsetToCharOffset(String text, int codePointOffset) {
        return text.offsetByCodePoints(0, codePointOffset);
    }

    /**
     * charオフセットをコードポイントオフセットに変換する。
     */
    static int charOffsetToCodePointOffset(String text, int charOffset) {
        return text.codePointCount(0, charOffset);
    }
}
