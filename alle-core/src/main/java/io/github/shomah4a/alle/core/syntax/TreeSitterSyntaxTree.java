package io.github.shomah4a.alle.core.syntax;

import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.treesitter.TSNode;
import org.treesitter.TSPoint;
import org.treesitter.TSTree;

/**
 * Tree-sitterのTSTreeをラップした{@link SyntaxTree}実装。
 *
 * <p>TSTreeへの参照を保持し、各メソッドでTSNodeを探索して{@link SyntaxNode}に変換する。
 * TSTreeのUTF-8バイト単位の位置情報はコードポイント単位に変換される。
 */
class TreeSitterSyntaxTree implements SyntaxTree {

    private final TSTree tree;
    private final ListIterable<String> lines;
    private final ImmutableSet<String> bracketTypes;

    TreeSitterSyntaxTree(TSTree tree, ListIterable<String> lines, ImmutableSet<String> bracketTypes) {
        this.tree = tree;
        this.lines = lines;
        this.bracketTypes = bracketTypes;
    }

    @Override
    public Optional<SyntaxNode> nodeAt(int line, int column) {
        TSNode root = tree.getRootNode();
        if (root.isNull()) {
            return Optional.empty();
        }
        int byteColumn = codePointColumnToUtf8Byte(line, column);
        TSPoint point = new TSPoint(line, byteColumn);
        TSNode descendant = root.getDescendantForPointRange(point, point);
        if (descendant.isNull()) {
            return Optional.empty();
        }
        return Optional.of(toSyntaxNode(descendant));
    }

    @Override
    public Optional<SyntaxNode> enclosingNodeOfType(int line, int column, String nodeType) {
        TSNode root = tree.getRootNode();
        if (root.isNull()) {
            return Optional.empty();
        }
        int byteColumn = codePointColumnToUtf8Byte(line, column);
        TSPoint point = new TSPoint(line, byteColumn);
        TSNode current = root.getDescendantForPointRange(point, point);
        while (!current.isNull()) {
            if (nodeType.equals(current.getType())) {
                return Optional.of(toSyntaxNode(current));
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    @Override
    public Optional<SyntaxNode> enclosingBracket(int line, int column) {
        TSNode root = tree.getRootNode();
        if (root.isNull()) {
            return Optional.empty();
        }
        int byteColumn = codePointColumnToUtf8Byte(line, column);
        TSPoint point = new TSPoint(line, byteColumn);
        TSNode current = root.getDescendantForPointRange(point, point);
        while (!current.isNull()) {
            if (bracketTypes.contains(current.getType())) {
                return Optional.of(toSyntaxNode(current));
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    @Override
    public SyntaxNode rootNode() {
        return toSyntaxNode(tree.getRootNode());
    }

    private SyntaxNode toSyntaxNode(TSNode node) {
        int childCount = node.getChildCount();
        MutableList<SyntaxNode> children = Lists.mutable.withInitialCapacity(childCount);
        for (int i = 0; i < childCount; i++) {
            TSNode child = node.getChild(i);
            if (!child.isNull()) {
                children.add(toSyntaxNode(child));
            }
        }
        int startLine = node.getStartPoint().getRow();
        int endLine = node.getEndPoint().getRow();
        int startCol = utf8ByteToCodePointColumn(startLine, node.getStartPoint().getColumn());
        int endCol = utf8ByteToCodePointColumn(endLine, node.getEndPoint().getColumn());
        return new SyntaxNode(node.getType(), startLine, startCol, endLine, endCol, children);
    }

    private int codePointColumnToUtf8Byte(int line, int codePointColumn) {
        if (line < 0 || line >= lines.size()) {
            return 0;
        }
        String lineText = lines.get(line);
        int byteOffset = 0;
        int cpCount = 0;
        int charIndex = 0;
        while (cpCount < codePointColumn && charIndex < lineText.length()) {
            int cp = lineText.codePointAt(charIndex);
            byteOffset += utf8ByteLength(cp);
            cpCount++;
            charIndex += Character.charCount(cp);
        }
        return byteOffset;
    }

    private int utf8ByteToCodePointColumn(int line, int utf8ByteOffset) {
        if (line < 0 || line >= lines.size()) {
            return 0;
        }
        String lineText = lines.get(line);
        int bytePos = 0;
        int cpCount = 0;
        int charIndex = 0;
        while (bytePos < utf8ByteOffset && charIndex < lineText.length()) {
            int cp = lineText.codePointAt(charIndex);
            bytePos += utf8ByteLength(cp);
            cpCount++;
            charIndex += Character.charCount(cp);
        }
        return cpCount;
    }

    private static int utf8ByteLength(int codePoint) {
        if (codePoint <= 0x7F) {
            return 1;
        } else if (codePoint <= 0x7FF) {
            return 2;
        } else if (codePoint <= 0xFFFF) {
            return 3;
        } else {
            return 4;
        }
    }
}
