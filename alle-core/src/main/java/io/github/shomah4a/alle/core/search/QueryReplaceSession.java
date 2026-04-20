package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.command.OverridingKeymapController;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.styling.FaceName;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * 対話型置換（query-replace / query-replace-regexp）のセッション状態を管理する。
 *
 * <p>i-search と同じく overriding keymap を利用してメインウィンドウへのフォーカスを
 * 維持したまま対話する。セッション開始時に最初のマッチを探索し、ユーザーキーに応じて
 * 置換 / スキップ / 一括置換 / キャンセルを行う。逐次再検索方式であり、事前の全マッチ
 * 列挙は行わない。
 */
public class QueryReplaceSession {

    private final Window window;
    private final BufferFacade buffer;
    private final MessageBuffer messageBuffer;
    private final OverridingKeymapController overridingKeymapController;
    private final int originalPoint;
    private final boolean regionActive;
    private final String from;
    private final String toTemplate;
    private final boolean regex;
    private final @Nullable Pattern pattern;

    private int rangeEnd;
    private int searchFrom;
    private int replacedCount;
    private @Nullable ReplaceMatch currentMatch;
    private boolean finished;

    private QueryReplaceSession(
            Window window,
            MessageBuffer messageBuffer,
            OverridingKeymapController overridingKeymapController,
            String from,
            String toTemplate,
            boolean regex,
            @Nullable Pattern pattern,
            int rangeStart,
            int rangeEnd,
            boolean regionActive) {
        this.window = window;
        this.buffer = window.getBuffer();
        this.messageBuffer = messageBuffer;
        this.overridingKeymapController = overridingKeymapController;
        this.from = from;
        this.toTemplate = toTemplate;
        this.regex = regex;
        this.pattern = pattern;
        this.originalPoint = window.getPoint();
        this.regionActive = regionActive;
        this.searchFrom = rangeStart;
        this.rangeEnd = rangeEnd;
        this.replacedCount = 0;
        this.finished = false;
    }

    /**
     * リテラル検索用セッションを生成する。
     */
    public static QueryReplaceSession forLiteral(
            Window window,
            MessageBuffer messageBuffer,
            OverridingKeymapController overridingKeymapController,
            String from,
            String toTemplate,
            int rangeStart,
            int rangeEnd,
            boolean regionActive) {
        return new QueryReplaceSession(
                window,
                messageBuffer,
                overridingKeymapController,
                from,
                toTemplate,
                false,
                null,
                rangeStart,
                rangeEnd,
                regionActive);
    }

    /**
     * 正規表現検索用セッションを生成する。
     */
    public static QueryReplaceSession forRegexp(
            Window window,
            MessageBuffer messageBuffer,
            OverridingKeymapController overridingKeymapController,
            Pattern pattern,
            String toTemplate,
            int rangeStart,
            int rangeEnd,
            boolean regionActive) {
        return new QueryReplaceSession(
                window,
                messageBuffer,
                overridingKeymapController,
                pattern.pattern(),
                toTemplate,
                true,
                pattern,
                rangeStart,
                rangeEnd,
                regionActive);
    }

    /**
     * セッションを開始する。最初のマッチを探索し、あれば overriding keymap を設定し
     * 対話を開始する。マッチがなければ即終了し "Replaced 0 occurrences" を表示する。
     */
    public void start() {
        var next = findNext();
        if (next.isEmpty()) {
            finish();
            return;
        }
        currentMatch = next.get();
        highlight(currentMatch);
        window.setPoint(currentMatch.start());
        var keymap = createKeymap();
        overridingKeymapController.set(keymap, this::handleUnboundKeyExit);
        updatePrompt();
    }

    /**
     * 現在のマッチを置換し、次のマッチへ進む（y / SPC）。
     * CommandLoop が本セッションを呼ぶコマンドの execute を withTransaction で
     * 包むため、ここでは個別にトランザクションを開始しない。1 コマンド = 1 undo 単位。
     * 途中で例外が発生した場合はセッションを終了し overriding keymap を解放する。
     */
    public void replaceCurrent() {
        if (finished || currentMatch == null) {
            return;
        }
        try {
            ReplaceMatch match = currentMatch;
            buffer.atomicOperation(buf -> {
                performReplacement(match);
                buf.markDirty();
                return null;
            });
            moveToNext();
        } catch (RuntimeException ex) {
            finishInternal(false);
            throw ex;
        }
    }

    /**
     * 現在のマッチをスキップし、次のマッチへ進む（n / DEL）。
     */
    public void skipCurrent() {
        if (finished || currentMatch == null) {
            return;
        }
        advanceAfterSkip(currentMatch);
        moveToNext();
    }

    /**
     * 現在位置以降の全マッチを無確認で置換する（!）。
     * CommandLoop が `!` コマンドの execute 全体を withTransaction で包むため、
     * このループ中のすべての置換が 1 undo 単位にまとまる。
     * 途中で例外が発生した場合はセッションを終了し overriding keymap を解放する。
     */
    public void replaceAllRemaining() {
        if (finished) {
            return;
        }
        try {
            buffer.atomicOperation(buf -> {
                while (currentMatch != null) {
                    performReplacement(currentMatch);
                    var next = findNext();
                    currentMatch = next.orElse(null);
                }
                buf.markDirty();
                return null;
            });
            finish();
        } catch (RuntimeException ex) {
            finishInternal(false);
            throw ex;
        }
    }

    /**
     * セッションをキャンセルする（C-g）。ここまでの置換は undo スタックに残す。
     */
    public void cancel() {
        if (finished) {
            return;
        }
        messageBuffer.message("Quit");
        finishInternal(false);
    }

    /**
     * 現在のマッチ（テスト用）。
     */
    public @Nullable ReplaceMatch getCurrentMatch() {
        return currentMatch;
    }

    /**
     * これまでに置換した件数（テスト用）。
     */
    public int getReplacedCount() {
        return replacedCount;
    }

    /**
     * セッションが終了しているか。
     */
    public boolean isFinished() {
        return finished;
    }

    private void handleUnboundKeyExit() {
        // overriding keymap に未バインドのキーが来たら確定終了する
        if (!finished) {
            finish();
        }
    }

    /**
     * トランザクション内で現在のマッチを置換する。呼び出し側が withTransaction 済みである前提。
     * Face のハイライトはバッファ変更時に自動で外れるため別途除去する必要はない。
     */
    private void performReplacement(ReplaceMatch match) {
        String replacement = replacementFor(match);
        int start = match.start();
        int matchedLen = match.end() - match.start();
        if (matchedLen > 0) {
            buffer.deleteText(start, matchedLen);
        }
        if (!replacement.isEmpty()) {
            buffer.insertText(start, replacement);
        }
        replacedCount++;

        int replacementLen = codePointLength(replacement);
        int delta = replacementLen - matchedLen;
        rangeEnd += delta;
        int newSearchFrom = start + replacementLen;
        if (newSearchFrom == start && matchedLen == 0) {
            // 空マッチ & 空置換で停留するのを防ぐ
            newSearchFrom = start + 1;
        }
        searchFrom = newSearchFrom;
    }

    private void moveToNext() {
        if (currentMatch != null) {
            clearHighlight(currentMatch);
        }
        var next = findNext();
        if (next.isEmpty()) {
            currentMatch = null;
            finish();
            return;
        }
        currentMatch = next.get();
        highlight(currentMatch);
        window.setPoint(currentMatch.start());
        updatePrompt();
    }

    private Optional<? extends ReplaceMatch> findNext() {
        String text = buffer.getText();
        int bufLen = buffer.length();
        int effectiveRangeEnd = Math.min(rangeEnd, bufLen);
        if (searchFrom > effectiveRangeEnd) {
            return Optional.empty();
        }
        if (regex) {
            return ReplaceEngine.findRegexpNext(text, Objects.requireNonNull(pattern), searchFrom, effectiveRangeEnd);
        }
        return ReplaceEngine.findLiteralNext(text, from, searchFrom, effectiveRangeEnd);
    }

    private String replacementFor(ReplaceMatch match) {
        if (match instanceof ReplaceMatch.Regex regex) {
            return ReplaceEngine.expandEmacsReplacement(toTemplate, regex);
        }
        return toTemplate;
    }

    private void advanceAfterSkip(ReplaceMatch match) {
        int newSearchFrom = match.end();
        int matchedLen = match.end() - match.start();
        if (matchedLen == 0) {
            newSearchFrom = match.start() + 1;
        }
        searchFrom = newSearchFrom;
    }

    private void highlight(ReplaceMatch match) {
        if (match.start() == match.end()) {
            return;
        }
        buffer.putFace(match.start(), match.end(), FaceName.QUERY_REPLACE_MATCH);
    }

    private void clearHighlight(ReplaceMatch match) {
        if (match.start() == match.end()) {
            return;
        }
        int bufLen = buffer.length();
        int end = Math.min(match.end(), bufLen);
        int start = Math.min(match.start(), end);
        if (start < end) {
            buffer.removeFaceByName(start, end, FaceName.QUERY_REPLACE_MATCH);
        }
    }

    private void finish() {
        finishInternal(true);
    }

    private void finishInternal(boolean reportCount) {
        if (finished) {
            return;
        }
        finished = true;
        if (currentMatch != null) {
            clearHighlight(currentMatch);
            currentMatch = null;
        }
        overridingKeymapController.clear();
        window.setPoint(clampToBuffer(originalPoint));
        if (regionActive) {
            window.clearMark();
        }
        if (reportCount) {
            messageBuffer.message("Replaced " + replacedCount + " occurrences");
        }
    }

    private int clampToBuffer(int offset) {
        int len = buffer.length();
        if (offset < 0) {
            return 0;
        }
        return Math.min(offset, len);
    }

    private void updatePrompt() {
        messageBuffer.message("Query replacing " + from + " with " + toTemplate + " (y, n, !, C-g to quit)");
    }

    private Keymap createKeymap() {
        var keymap = new Keymap("query-replace");
        var yes = new QueryReplaceYesCommand(this);
        var no = new QueryReplaceNoCommand(this);
        var all = new QueryReplaceAllCommand(this);
        var cancel = new QueryReplaceCancelCommand(this);

        keymap.bind(KeyStroke.of('y'), yes);
        keymap.bind(KeyStroke.of(' '), yes);
        keymap.bind(KeyStroke.of('n'), no);
        keymap.bind(KeyStroke.of(0x7F), no); // DEL
        keymap.bind(KeyStroke.of('!'), all);
        keymap.bind(KeyStroke.ctrl('g'), cancel);
        return keymap;
    }

    private static int codePointLength(String s) {
        return (int) s.codePoints().count();
    }
}
