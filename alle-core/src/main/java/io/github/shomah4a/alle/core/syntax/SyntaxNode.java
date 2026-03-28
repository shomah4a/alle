package io.github.shomah4a.alle.core.syntax;

import org.eclipse.collections.api.list.ListIterable;

/**
 * 構文木のノードを表す値型。
 *
 * <p>{@code type} はパーサー実装固有のノードタイプ名である。
 * 例えばTree-sitterの場合は "function_definition", "identifier" 等の値を取る。
 *
 * @param type パーサー実装固有のノードタイプ名
 * @param startLine 開始行（0始まり）
 * @param startColumn 開始カラム（0始まり、コードポイント単位）
 * @param endLine 終了行（0始まり）
 * @param endColumn 終了カラム（0始まり、コードポイント単位、排他）
 * @param children 子ノードのリスト
 */
public record SyntaxNode(
        String type, int startLine, int startColumn, int endLine, int endColumn, ListIterable<SyntaxNode> children) {}
