package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.command.OverridingKeymapController;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.styling.FaceName;
import io.github.shomah4a.alle.core.util.StringMatching;
import io.github.shomah4a.alle.core.window.Window;
import org.jspecify.annotations.Nullable;

/**
 * i-searchのセッション状態を管理する。
 * クエリ文字列、検索方向、マッチ位置、ハイライトを保持し、
 * overriding keymapを通じてCommandLoopを制御する。
 */
public class ISearchSession {

    private final Window window;
    private final BufferFacade buffer;
    private final MessageBuffer messageBuffer;
    private final OverridingKeymapController overridingKeymapController;
    private final ISearchHistory history;
    private final int originalPoint;
    private final StringBuilder query;
    private boolean forward;
    private @Nullable SearchResult currentMatch;
    private boolean failed;

    public ISearchSession(
            Window window,
            MessageBuffer messageBuffer,
            OverridingKeymapController overridingKeymapController,
            ISearchHistory history,
            boolean forward) {
        this.window = window;
        this.buffer = window.getBuffer();
        this.messageBuffer = messageBuffer;
        this.overridingKeymapController = overridingKeymapController;
        this.history = history;
        this.originalPoint = window.getPoint();
        this.query = new StringBuilder();
        this.forward = forward;
        this.failed = false;
    }

    /**
     * i-searchを開始する。overriding keymapを設定し、エコー表示を開始する。
     */
    public void start() {
        var keymap = createKeymap();
        overridingKeymapController.set(keymap, this::confirmExit);
        updateEcho();
    }

    /**
     * クエリに文字を追加し、インクリメンタル検索を実行する。
     */
    public void appendChar(int codePoint) {
        query.appendCodePoint(codePoint);
        searchIncremental();
    }

    /**
     * クエリ末尾の文字を削除し、再検索する。
     * クエリが空になった場合はマッチをクリアする。
     */
    public void deleteChar() {
        if (query.isEmpty()) {
            return;
        }
        int lastIndex = query.length() - Character.charCount(query.codePointBefore(query.length()));
        query.delete(lastIndex, query.length());

        if (query.isEmpty()) {
            clearHighlight();
            currentMatch = null;
            failed = false;
            window.setPoint(originalPoint);
        } else {
            // 元の位置から再検索
            searchFrom(originalPoint);
        }
        updateEcho();
    }

    /**
     * 次のマッチに移動する（C-s）。
     * クエリが空の場合は前回クエリを使用する。
     */
    public void searchNext() {
        if (query.isEmpty() && !history.getLastQuery().isEmpty()) {
            query.append(history.getLastQuery());
        }
        if (query.isEmpty()) {
            return;
        }
        forward = true;
        int searchFrom = currentMatch != null ? currentMatch.start() + 1 : window.getPoint();
        searchFrom(searchFrom);
        updateEcho();
    }

    /**
     * 前のマッチに移動する（C-r）。
     * クエリが空の場合は前回クエリを使用する。
     */
    public void searchPrevious() {
        if (query.isEmpty() && !history.getLastQuery().isEmpty()) {
            query.append(history.getLastQuery());
        }
        if (query.isEmpty()) {
            return;
        }
        forward = false;
        int searchFrom = currentMatch != null ? currentMatch.start() : window.getPoint();
        searchFrom(searchFrom);
        updateEcho();
    }

    /**
     * 検索を確定する（RET）。カーソルを現在位置に残し、ハイライトを除去する。
     */
    public void confirm() {
        saveQuery();
        clearHighlight();
        overridingKeymapController.clear();
        messageBuffer.message("");
    }

    /**
     * 検索をキャンセルする（C-g）。元の位置に戻し、ハイライトを除去する。
     */
    public void cancel() {
        clearHighlight();
        window.setPoint(originalPoint);
        overridingKeymapController.clear();
        messageBuffer.message("Quit");
    }

    /**
     * 現在のクエリを返す。テスト用。
     */
    public String getQuery() {
        return query.toString();
    }

    /**
     * 現在のマッチ結果を返す。テスト用。
     */
    public @Nullable SearchResult getCurrentMatch() {
        return currentMatch;
    }

    private void confirmExit() {
        saveQuery();
        clearHighlight();
        messageBuffer.message("");
    }

    private void searchIncremental() {
        searchFrom(originalPoint);
        updateEcho();
    }

    private void searchFrom(int fromCodePointOffset) {
        String text = buffer.getText();
        String queryStr = query.toString();
        // smart-case: クエリに大文字を含むときのみ case sensitive。Emacs 風挙動。
        boolean caseSensitive = StringMatching.containsUpperCase(queryStr);

        var result = forward
                ? BufferSearcher.searchForward(text, queryStr, fromCodePointOffset, caseSensitive)
                : BufferSearcher.searchBackward(text, queryStr, fromCodePointOffset, caseSensitive);

        clearHighlight();

        if (result.isPresent()) {
            currentMatch = result.get();
            failed = false;
            buffer.putFace(currentMatch.start(), currentMatch.end(), FaceName.ISEARCH_MATCH);
            window.setPoint(forward ? currentMatch.end() : currentMatch.start());
        } else {
            currentMatch = null;
            failed = true;
        }
    }

    private void clearHighlight() {
        if (currentMatch != null) {
            buffer.removeFaceByName(currentMatch.start(), currentMatch.end(), FaceName.ISEARCH_MATCH);
        }
    }

    private void saveQuery() {
        if (!query.isEmpty()) {
            history.setLastQuery(query.toString());
        }
    }

    private void updateEcho() {
        String prefix;
        if (failed) {
            prefix = "Failing ";
        } else if (currentMatch != null && currentMatch.wrapped()) {
            prefix = "Wrapped ";
        } else {
            prefix = "";
        }
        String direction = forward ? "I-search" : "I-search backward";
        messageBuffer.message(prefix + direction + ": " + query);
    }

    private Keymap createKeymap() {
        var keymap = new Keymap("isearch");

        // 印字可能文字 → クエリに追加
        keymap.setDefaultCommand(new ISearchSelfInsertCommand(this));

        // C-s → 次のマッチ
        keymap.bind(KeyStroke.ctrl('s'), new ISearchNextCommand(this));

        // C-r → 前のマッチ
        keymap.bind(KeyStroke.ctrl('r'), new ISearchPreviousCommand(this));

        // RET → 確定
        keymap.bind(KeyStroke.of('\n'), new ISearchConfirmCommand(this));
        keymap.bind(KeyStroke.of('\r'), new ISearchConfirmCommand(this));

        // DEL/Backspace → 文字削除
        keymap.bind(KeyStroke.of(0x7F), new ISearchDeleteCharCommand(this));

        // C-g → キャンセル（明示バインドでquitCommandフォールバックより優先）
        keymap.bind(KeyStroke.ctrl('g'), new ISearchCancelCommand(this));

        return keymap;
    }
}
