package io.github.shomah4a.alle.core.styling;

import java.util.regex.Pattern;

/**
 * スタイリングルール。パターンとFaceNameのペアを宣言的に定義する。
 * スクリプト拡張からルールリストを渡すだけでモードのスタイリングを定義可能。
 */
public sealed interface StylingRule {

    /**
     * 行全体にマッチするルール。パターンが行にマッチした場合、行全体にFaceNameを適用する。
     */
    record LineMatch(Pattern pattern, FaceName faceName) implements StylingRule {}

    /**
     * 部分パターンマッチルール。パターンにマッチした部分にFaceNameを適用する。
     * 1行に複数マッチすることがある。
     */
    record PatternMatch(Pattern pattern, FaceName faceName) implements StylingRule {}

    /**
     * リージョンマッチルール。開始パターンから終了パターンまでの複数行にまたがる領域にFaceNameを適用する。
     * 開始パターンと終了パターンが同一行内にある場合はその範囲のみ適用する。
     * リージョン内では他のルールは無視される。
     */
    record RegionMatch(Pattern open, Pattern close, FaceName faceName) implements StylingRule {}
}
