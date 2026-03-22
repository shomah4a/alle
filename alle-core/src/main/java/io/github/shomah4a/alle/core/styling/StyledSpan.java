package io.github.shomah4a.alle.core.styling;

/**
 * 行内の範囲とスタイルのペア。
 *
 * @param start 開始位置（コードポイント単位、inclusive）
 * @param end 終了位置（コードポイント単位、exclusive）
 * @param face スタイル
 */
public record StyledSpan(int start, int end, Face face) {}
