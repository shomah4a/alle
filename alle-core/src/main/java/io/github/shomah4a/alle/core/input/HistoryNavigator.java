package io.github.shomah4a.alle.core.input;

import java.util.Optional;

/**
 * ミニバッファの入力履歴をナビゲートするセッションスコープのオブジェクト。
 * プロンプトセッション開始時に生成し、セッション終了時に破棄する。
 * ナビゲーション開始前の入力（元入力）を保持し、末尾を超えた場合に復元する。
 */
public class HistoryNavigator {

    private final InputHistory history;
    private String originalInput;

    /**
     * カーソル位置。history.size() の場合は元入力を指す（初期状態）。
     */
    private int cursor;

    public HistoryNavigator(InputHistory history, String originalInput) {
        this.history = history;
        this.originalInput = originalInput;
        this.cursor = history.size();
    }

    /**
     * 元入力を更新する。
     * 初回ナビゲーション時に現在のミニバッファ入力を保存するために使用する。
     */
    public void updateOriginalInput(String originalInput) {
        this.originalInput = originalInput;
    }

    /**
     * 1つ前の履歴に移動する。
     * 先頭に達している場合は empty を返す。
     */
    public Optional<String> previous() {
        if (cursor <= 0) {
            return Optional.empty();
        }
        cursor--;
        return Optional.of(history.get(cursor));
    }

    /**
     * 1つ次の履歴に移動する。
     * 末尾を超えた場合は元入力を返す。
     * 既に元入力位置にいる場合は empty を返す。
     */
    public Optional<String> next() {
        if (cursor >= history.size()) {
            return Optional.empty();
        }
        cursor++;
        if (cursor >= history.size()) {
            return Optional.of(originalInput);
        }
        return Optional.of(history.get(cursor));
    }
}
