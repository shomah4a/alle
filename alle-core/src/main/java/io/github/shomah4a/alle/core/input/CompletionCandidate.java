package io.github.shomah4a.alle.core.input;

/**
 * 補完候補を表す。
 *
 * @param value    補完値（ミニバッファに挿入される文字列）
 * @param label    表示用ラベル（*Completions*バッファに表示される文字列）
 * @param terminal trueなら確定可能な候補（例: ファイル）、falseなら継続補完（例: ディレクトリ）
 */
public record CompletionCandidate(String value, String label, boolean terminal) {

    /**
     * 確定可能な候補を作成する。labelはvalueと同じ。
     */
    public static CompletionCandidate terminal(String value) {
        return new CompletionCandidate(value, value, true);
    }

    /**
     * 確定可能な候補を表示用ラベル付きで作成する。
     */
    public static CompletionCandidate terminal(String value, String label) {
        return new CompletionCandidate(value, label, true);
    }

    /**
     * 継続補完用の候補を作成する。labelはvalueと同じ。
     */
    public static CompletionCandidate partial(String value) {
        return new CompletionCandidate(value, value, false);
    }

    /**
     * 継続補完用の候補を表示用ラベル付きで作成する。
     */
    public static CompletionCandidate partial(String value, String label) {
        return new CompletionCandidate(value, label, false);
    }
}
