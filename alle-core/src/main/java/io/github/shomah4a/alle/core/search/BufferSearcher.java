package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.util.StringMatching;
import java.util.Optional;

/**
 * バッファ内テキスト検索エンジン。
 * buffer.getText()で取得した文字列に対して {@link StringMatching#indexOf}
 * / {@link StringMatching#lastIndexOf} で検索する。
 * char offsetとcodepoint offsetの変換を内部で行う。
 *
 * <p>{@code caseSensitive} 引数で大文字小文字の扱いを切り替える。
 * smart-case 判定は呼び出し側で {@link StringMatching#containsUpperCase} を用いて行うこと。
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
     * @param caseSensitive true なら大文字小文字を区別する。
     *                      false の場合 {@link StringMatching} の case folding 規則に従う
     * @return 検索結果。クエリが空またはマッチなしの場合はempty
     */
    public static Optional<SearchResult> searchForward(
            String text, String query, int fromCodePointOffset, boolean caseSensitive) {
        if (query.isEmpty()) {
            return Optional.empty();
        }

        boolean ignoreCase = !caseSensitive;
        int fromCharOffset = codePointOffsetToCharOffset(text, fromCodePointOffset);
        int charIndex = StringMatching.indexOf(text, query, fromCharOffset, ignoreCase);

        if (charIndex >= 0) {
            return Optional.of(toSearchResult(text, query, charIndex, false));
        }

        // ラップアラウンド: 先頭から検索開始位置までを検索
        charIndex = StringMatching.indexOf(text, query, 0, ignoreCase);
        if (charIndex >= 0 && charIndex < fromCharOffset) {
            return Optional.of(toSearchResult(text, query, charIndex, true));
        }

        return Optional.empty();
    }

    /**
     * 指定位置から後方検索する。
     * マッチが見つからない場合はバッファ末尾からラップアラウンドする。
     *
     * <p>{@code fromCodePointOffset} 自身は検索範囲に含まない。マッチ開始位置が
     * {@code fromCodePointOffset - 1} 以下となる最大位置を探す。
     * これにより、現在マッチ位置から searchBackward を再呼び出ししたときに
     * 同じ位置を返さないことを保証する。
     *
     * @param text バッファ全体のテキスト
     * @param query 検索クエリ
     * @param fromCodePointOffset 検索開始位置（コードポイント単位、この位置自身は含まない）
     * @param caseSensitive true なら大文字小文字を区別する。
     *                      false の場合 {@link StringMatching} の case folding 規則に従う
     * @return 検索結果。クエリが空またはマッチなしの場合はempty
     */
    public static Optional<SearchResult> searchBackward(
            String text, String query, int fromCodePointOffset, boolean caseSensitive) {
        if (query.isEmpty()) {
            return Optional.empty();
        }

        boolean ignoreCase = !caseSensitive;
        int fromCharOffset = codePointOffsetToCharOffset(text, fromCodePointOffset);
        // fromCharOffsetより前で最後に見つかるマッチを探す
        int charIndex = StringMatching.lastIndexOf(text, query, fromCharOffset - 1, ignoreCase);

        if (charIndex >= 0) {
            return Optional.of(toSearchResult(text, query, charIndex, false));
        }

        // ラップアラウンド: バッファ末尾から検索
        charIndex = StringMatching.lastIndexOf(text, query, text.length(), ignoreCase);
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
