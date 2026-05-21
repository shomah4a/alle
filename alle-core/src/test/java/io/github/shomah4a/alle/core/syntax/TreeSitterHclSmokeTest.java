package io.github.shomah4a.alle.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.Test;
import org.treesitter.TSNode;
import org.treesitter.TreeSitterHcl;

/**
 * bonede tree-sitter-hcl 1.1.0a と tree-sitter core 0.26.6 のABI互換性、
 * および HCL grammar の実ノード型を検証するスモークテスト。
 *
 * <p>HCL grammar は他の言語と公開タイミングが異なり、ノード名（{@code numeric_lit}/{@code bool_lit}/
 * {@code block}/{@code object}/{@code tuple} 等）が固定されている保証はないため、
 * 実 AST 上でこれらが取れることを担保する。
 */
class TreeSitterHclSmokeTest {

    private static final String SAMPLE_HCL = """
            # top level comment
            resource "aws_instance" "web" {
              ami           = "ami-123"
              instance_type = "t2.micro"
              count         = 2
              enabled       = true
              tags = {
                Name = "web"
              }
              network_ids = [1, 2, 3]
              description = lower("Hello")
            }
            """;

    @Test
    void TreeSitterHclがインスタンス化できる() {
        var lang = new TreeSitterHcl();
        assertNotNull(lang);
    }

    @Test
    void HCLサンプルがパースできて主要ノード型が出現する() {
        var session = new TreeSitterSession(new TreeSitterHcl());
        var tree = session.parse(Lists.immutable.of(SAMPLE_HCL.split("\n", -1)));
        TSNode root = tree.getRootNode();
        assertNotNull(root);
        assertEquals("config_file", root.getType(), "ルートノードはconfig_file");
        assertNotEquals(0, root.getChildCount(), "子ノードが存在する");

        MutableList<String> nodeTypes = Lists.mutable.empty();
        collectNodeTypes(root, nodeTypes);

        assertTrue(nodeTypes.contains("body"), "bodyノードが存在する: types=" + nodeTypes);
        assertTrue(nodeTypes.contains("block"), "blockノードが存在する: types=" + nodeTypes);
        assertTrue(nodeTypes.contains("attribute"), "attributeノードが存在する: types=" + nodeTypes);
        assertTrue(nodeTypes.contains("object"), "objectノードが存在する: types=" + nodeTypes);
        assertTrue(nodeTypes.contains("tuple"), "tupleノードが存在する: types=" + nodeTypes);
        assertTrue(nodeTypes.contains("function_call"), "function_callノードが存在する: types=" + nodeTypes);
        assertTrue(nodeTypes.contains("numeric_lit"), "numeric_litノードが存在する: types=" + nodeTypes);
        assertTrue(nodeTypes.contains("bool_lit"), "bool_litノードが存在する: types=" + nodeTypes);
        assertTrue(nodeTypes.contains("comment"), "commentノードが存在する: types=" + nodeTypes);
    }

    @Test
    void block_objectのラッパーノード構造が維持されている() {
        // ADR 0136 の HCL_BRACKET_TYPES=空集合 という判断の前提として、
        // `{` `[` `(` がラッパーノード経由で表現されていることを固定する。
        // 将来 grammar が更新されてラッパー構造が変わった場合は本テストが失敗し、
        // CStyleIndentState のインデント方針を見直すべきシグナルとなる。
        var session = new TreeSitterSession(new TreeSitterHcl());
        var tree = session.parse(Lists.immutable.of(SAMPLE_HCL.split("\n", -1)));
        TSNode root = tree.getRootNode();

        MutableList<String> nodeTypes = Lists.mutable.empty();
        collectNodeTypes(root, nodeTypes);

        assertTrue(nodeTypes.contains("block_start"), "block_startラッパーが存在する: types=" + nodeTypes);
        assertTrue(nodeTypes.contains("block_end"), "block_endラッパーが存在する: types=" + nodeTypes);
        assertTrue(nodeTypes.contains("object_start"), "object_startラッパーが存在する: types=" + nodeTypes);
        assertTrue(nodeTypes.contains("object_end"), "object_endラッパーが存在する: types=" + nodeTypes);
        assertTrue(nodeTypes.contains("tuple_start"), "tuple_startラッパーが存在する: types=" + nodeTypes);
        assertTrue(nodeTypes.contains("tuple_end"), "tuple_endラッパーが存在する: types=" + nodeTypes);
    }

    private static void collectNodeTypes(TSNode node, MutableList<String> result) {
        result.add(node.getType());
        for (int i = 0; i < node.getChildCount(); i++) {
            collectNodeTypes(node.getChild(i), result);
        }
    }
}
