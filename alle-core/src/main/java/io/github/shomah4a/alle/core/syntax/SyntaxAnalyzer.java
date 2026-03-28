package io.github.shomah4a.alle.core.syntax;

import org.eclipse.collections.api.list.ListIterable;

/**
 * 構文解析器インターフェイス。
 *
 * <p>ドキュメントの行リストを受け取り、構文解析結果（{@link SyntaxTree}）を返す。
 * 実装側はキャッシュやインクリメンタルパースを内部で管理してよい。
 *
 * <p>インスタンスはバッファごとのモードが保持するため、ステートのスコープはバッファに閉じている。
 */
public interface SyntaxAnalyzer {

    /**
     * ドキュメント全体を解析し、構文木を返す。
     *
     * @param lines 各行のテキスト（改行文字を含まない）
     * @return 構文解析結果
     */
    SyntaxTree analyze(ListIterable<String> lines);
}
