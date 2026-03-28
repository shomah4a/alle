package io.github.shomah4a.alle.core.styling;

/**
 * 行内の範囲とセマンティック名のペア。
 *
 * @param start 開始位置（コードポイント単位、inclusive）
 * @param end 終了位置（コードポイント単位、exclusive）
 * @param faceName セマンティック名
 */
public record StyledSpan(int start, int end, FaceName faceName) {}
