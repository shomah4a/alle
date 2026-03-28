package io.github.shomah4a.alle.core.syntax;

import io.github.shomah4a.alle.core.styling.NodeFaceMapping;
import org.eclipse.collections.api.set.ImmutableSet;
import org.treesitter.TSLanguage;

/**
 * 言語ごとのTree-sitter設定を保持する値型。
 *
 * @param language Tree-sitterの言語定義
 * @param queryString ハイライト用S式クエリ文字列
 * @param captureMapping キャプチャ名からFaceNameへのマッピング
 * @param bracketTypes 括弧系ノードとみなすノードタイプ名の集合
 */
public record TreeSitterLanguageConfig(
        TSLanguage language, String queryString, NodeFaceMapping captureMapping, ImmutableSet<String> bracketTypes) {}
