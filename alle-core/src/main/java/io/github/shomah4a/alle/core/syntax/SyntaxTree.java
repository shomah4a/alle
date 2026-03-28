package io.github.shomah4a.alle.core.syntax;

import java.util.Optional;

/**
 * 構文解析結果を表すインターフェイス。
 *
 * <p>パーサー実装に依存しない構文木への問い合わせAPIを提供する。
 * ノードのタイプ名はパーサー実装固有の値であるため、
 * 利用側はパーサーが返すタイプ名を前提としたコードを書く必要がある。
 */
public interface SyntaxTree {

    /**
     * 指定位置の最も内側のノードを返す。
     *
     * @param line 行番号（0始まり）
     * @param column カラム（0始まり、コードポイント単位）
     * @return 該当ノード。位置が範囲外の場合はempty
     */
    Optional<SyntaxNode> nodeAt(int line, int column);

    /**
     * 指定位置を囲む最も内側の、指定タイプのノードを返す。
     *
     * @param line 行番号（0始まり）
     * @param column カラム（0始まり、コードポイント単位）
     * @param nodeType パーサー実装固有のノードタイプ名
     * @return 該当ノード。見つからない場合はempty
     */
    Optional<SyntaxNode> enclosingNodeOfType(int line, int column, String nodeType);

    /**
     * 指定位置を囲む最も内側の括弧系ノードを返す。
     *
     * <p>括弧系ノードとは、丸括弧 {@code ()}、角括弧 {@code []}、波括弧 {@code {}} に
     * 対応するノードを指す。具体的にどのノードタイプが括弧とみなされるかは実装依存。
     *
     * @param line 行番号（0始まり）
     * @param column カラム（0始まり、コードポイント単位）
     * @return 括弧ノード。見つからない場合はempty
     */
    Optional<SyntaxNode> enclosingBracket(int line, int column);

    /**
     * 構文木のルートノードを返す。
     *
     * @return ルートノード
     */
    SyntaxNode rootNode();
}
