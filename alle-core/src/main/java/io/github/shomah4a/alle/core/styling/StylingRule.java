package io.github.shomah4a.alle.core.styling;

import java.util.regex.Pattern;

/**
 * スタイリングルール。パターンとFaceのペアを宣言的に定義する。
 * スクリプト拡張からルールリストを渡すだけでモードのスタイリングを定義可能。
 */
public sealed interface StylingRule {

    /**
     * 行全体にマッチするルール。パターンが行にマッチした場合、行全体にFaceを適用する。
     */
    record LineMatch(Pattern pattern, Face face) implements StylingRule {}

    /**
     * 部分パターンマッチルール。パターンにマッチした部分にFaceを適用する。
     * 1行に複数マッチすることがある。
     */
    record PatternMatch(Pattern pattern, Face face) implements StylingRule {}

    /**
     * リージョンマッチルール。開始パターンから終了パターンまでの複数行にまたがる領域にFaceを適用する。
     * 開始パターンと終了パターンが同一行内にある場合はその範囲のみ適用する。
     * リージョン内では他のルールは無視される。
     */
    record RegionMatch(Pattern open, Pattern close, Face face) implements StylingRule {}
}
