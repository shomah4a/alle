package io.github.shomah4a.alle.core.styling;

import org.eclipse.collections.api.list.ListIterable;

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
}
