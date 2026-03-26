package io.github.shomah4a.alle.core.input;

import org.eclipse.collections.api.list.ListIterable;
import org.jspecify.annotations.Nullable;

/**
 * 補完候補リストと選択状態を管理する。
 * テキスト表示上のカーソル位置ではなく、インデックスベースで候補選択を追跡する。
 */
public class CompletionsModel {

    private final ListIterable<String> candidates;
    private int selectedIndex;

    /**
     * 候補リストを指定してモデルを作成する。
     * 初期状態では何も選択されていない（selectedIndex = -1）。
     *
     * @param candidates 補完候補リスト（1件以上）
     * @throws IllegalArgumentException 候補が空の場合
     */
    public CompletionsModel(ListIterable<String> candidates) {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }
        this.candidates = candidates;
        this.selectedIndex = -1;
    }

    /**
     * 補完候補リストを返す。
     */
    public ListIterable<String> getCandidates() {
        return candidates;
    }

    /**
     * 現在の選択インデックスを返す。
     * 未選択の場合は -1。
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * 次の候補を選択する。
     * 未選択状態からは先頭を選択する。
     * 末尾の場合は先頭に戻る。
     */
    public void selectNext() {
        selectedIndex = (selectedIndex + 1) % candidates.size();
    }

    /**
     * 前の候補を選択する。
     * 未選択状態からは末尾を選択する。
     * 先頭の場合は末尾に戻る。
     */
    public void selectPrevious() {
        if (selectedIndex <= 0) {
            selectedIndex = candidates.size() - 1;
        } else {
            selectedIndex--;
        }
    }

    /**
     * 選択中の候補を返す。
     * 未選択の場合は null。
     */
    public @Nullable String getSelectedCandidate() {
        if (selectedIndex < 0) {
            return null;
        }
        return candidates.get(selectedIndex);
    }

    /**
     * 候補リストを表示用テキストに整形する。
     * 選択中の候補には先頭に ">" マークを付与する。
     */
    public String formatForDisplay() {
        var sb = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            if (i == selectedIndex) {
                sb.append("> ");
            } else {
                sb.append("  ");
            }
            sb.append(candidates.get(i));
        }
        return sb.toString();
    }
}
