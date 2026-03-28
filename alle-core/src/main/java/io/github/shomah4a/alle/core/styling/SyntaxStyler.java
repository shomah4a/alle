package io.github.shomah4a.alle.core.styling;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

/**
 * 行テキストからスタイル情報を生成するインターフェース。
 * 返されるStyledSpanリストはstart順にソートされ、重複がないことを保証する。
 */
public interface SyntaxStyler {

    /**
     * 行テキストをスタイリングしてStyledSpanのリストを返す。
     * スパンに含まれないコードポイントはデフォルトスタイルで描画される。
     */
    ListIterable<StyledSpan> styleLine(String lineText);

    /**
     * 前行からの状態を受け取り、行テキストをスタイリングして結果と次行の状態を返す。
     * マルチラインスタイリングに対応する場合はこのメソッドをオーバーライドする。
     */
    default StylingResult styleLineWithState(String lineText, StylingState state) {
        return new StylingResult(styleLine(lineText), StylingState.NONE);
    }

    /**
     * スタイリングの初期状態を返す。
     */
    default StylingState initialState() {
        return StylingState.NONE;
    }

    /**
     * ドキュメント全体をスタイリングし、行ごとのスパンリストを返す。
     *
     * <p>デフォルト実装は {@link #styleLineWithState} を行ごとに呼び出して結果を組み立てる。
     * パーサーベースのスタイラーはこのメソッドをオーバーライドしてドキュメント全体を一括処理する。
     *
     * @param lines 各行のテキスト（改行文字を含まない）
     * @return 行ごとのスパンリスト（外側リストのインデックスが行番号に対応、サイズは入力行数と一致）
     */
    default ListIterable<ListIterable<StyledSpan>> styleDocument(ListIterable<String> lines) {
        MutableList<ListIterable<StyledSpan>> result = Lists.mutable.withInitialCapacity(lines.size());
        StylingState state = initialState();

        for (String lineText : lines) {
            var stylingResult = styleLineWithState(lineText, state);
            result.add(stylingResult.spans());
            state = stylingResult.nextState();
        }

        return result;
    }
}
