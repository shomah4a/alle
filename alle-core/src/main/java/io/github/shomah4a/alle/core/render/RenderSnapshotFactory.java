package io.github.shomah4a.alle.core.render;

import io.github.shomah4a.alle.core.DisplayWidthUtil;
import io.github.shomah4a.alle.core.VisualLineUtil;
import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.statusline.StatusLineContext;
import io.github.shomah4a.alle.core.statusline.StatusLineRenderer;
import io.github.shomah4a.alle.core.statusline.StatusLineSettings;
import io.github.shomah4a.alle.core.styling.StyledSpan;
import io.github.shomah4a.alle.core.styling.SyntaxStyler;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.LayoutResult;
import io.github.shomah4a.alle.core.window.Rect;
import io.github.shomah4a.alle.core.window.ViewportSize;
import io.github.shomah4a.alle.core.window.Window;
import io.github.shomah4a.alle.core.window.WindowLayout;
import java.util.Optional;
import java.util.OptionalInt;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.jspecify.annotations.Nullable;

/**
 * Frameの状態からimmutableな描画スナップショットを生成するファクトリ。
 *
 * <p>ensurePointVisible等の副作用を伴うスクロール位置調整もここで行う。
 * ロジックスレッドから呼び出される想定。
 */
public final class RenderSnapshotFactory {

    private RenderSnapshotFactory() {}

    /**
     * Frameの状態からimmutableな描画スナップショットを作成する。
     *
     * <p>副作用: 各ウィンドウのdisplayStartLine/displayStartColumnを
     * カーソルが表示範囲内に収まるよう調整する。
     *
     * @param frame エディタのフレーム
     * @param messageBuffer メッセージバッファ（エコーエリア表示用）
     * @param cols 画面の列数
     * @param rows 画面の行数
     * @return 描画スナップショット
     */
    public static RenderSnapshot create(Frame frame, MessageBuffer messageBuffer, int cols, int rows) {
        return create(frame, messageBuffer, cols, rows, null);
    }

    /**
     * StatusLineRenderer を指定してスナップショットを生成する。
     * rendererがnullの場合はフォールバック（従来のハードコード生成）を使用する。
     */
    public static RenderSnapshot create(
            Frame frame,
            MessageBuffer messageBuffer,
            int cols,
            int rows,
            @Nullable StatusLineRenderer statusLineRenderer) {
        int windowAreaRows = rows - 1;
        int minibufferRow = rows - 1;

        var treeArea = new Rect(0, 0, cols, windowAreaRows);
        var layoutResult = WindowLayout.compute(frame.getWindowTree(), treeArea);

        var windowSnapshots = Lists.mutable.<RenderSnapshot.WindowSnapshot>empty();

        layoutResult.windowRects().forEachKeyValue((window, rect) -> {
            if (rect.height() < 2) {
                return;
            }
            int bufferRows = rect.height() - 1;
            window.setViewportSize(new ViewportSize(bufferRows, rect.width()));

            var buffer = window.getBuffer();
            int tabWidth = buffer.getSettings().get(EditorSettings.TAB_WIDTH);

            boolean truncateLines = window.isTruncateLines();
            if (truncateLines) {
                window.ensurePointVisible(bufferRows);
            } else {
                ensurePointVisibleWrapped(window, bufferRows, rect.width(), tabWidth);
            }
            window.ensurePointHorizontallyVisible(rect.width());

            int displayStart = window.getDisplayStartLine();
            int displayStartColumn = window.getDisplayStartColumn();

            // リージョン範囲（バッファオフセット）を事前計算
            var regionStart = window.getRegionStart();
            var regionEnd = window.getRegionEnd();

            var visibleLines = Lists.mutable.<RenderSnapshot.LineSnapshot>empty();
            if (truncateLines) {
                buildTruncatedVisibleLines(buffer, displayStart, bufferRows, regionStart, regionEnd, visibleLines);
            } else {
                int displayStartVl = window.getDisplayStartVisualLine();
                buildWrappedVisibleLines(
                        buffer,
                        displayStart,
                        displayStartVl,
                        bufferRows,
                        rect.width(),
                        regionStart,
                        regionEnd,
                        tabWidth,
                        visibleLines);
            }

            String modeLine = buildModeLineText(window, statusLineRenderer);
            OptionalInt highlightLine = OptionalInt.empty();
            if (window.isHighlightPointLine()) {
                int pointLine = buffer.lineIndexForOffset(window.getPoint());
                if (truncateLines) {
                    int relativeLine = pointLine - displayStart;
                    if (relativeLine >= 0 && relativeLine < visibleLines.size()) {
                        highlightLine = OptionalInt.of(relativeLine);
                    }
                } else {
                    highlightLine = computeWrappedHighlightLine(
                            window, displayStart, pointLine, rect.width(), visibleLines.size(), tabWidth);
                }
            }
            Optional<RenderSnapshot.RegionRange> regionRange = window.getRegionStart()
                    .flatMap(start -> window.getRegionEnd().map(end -> new RenderSnapshot.RegionRange(start, end)));
            windowSnapshots.add(new RenderSnapshot.WindowSnapshot(
                    rect,
                    visibleLines,
                    displayStartColumn,
                    truncateLines,
                    modeLine,
                    highlightLine,
                    regionRange,
                    tabWidth));
        });

        // ミニバッファ / エコーエリア
        RenderSnapshot.MinibufferSnapshot minibufferSnapshot;
        int defaultTabWidth = EditorSettings.TAB_WIDTH.defaultValue();
        if (frame.isMinibufferActive()) {
            var minibufferWindow = frame.getMinibufferWindow();
            minibufferWindow.ensurePointHorizontallyVisible(cols);
            var mbBuffer = minibufferWindow.getBuffer();
            int mbTabWidth = mbBuffer.getSettings().get(EditorSettings.TAB_WIDTH);
            String text = mbBuffer.length() > 0 ? mbBuffer.getText() : "";
            var mbFaceSpans = mbBuffer.getFaceSpans(0, mbBuffer.length());
            var mbSpansOpt = mbFaceSpans.isEmpty()
                    ? Optional.<ListIterable<StyledSpan>>empty()
                    : Optional.<ListIterable<StyledSpan>>of(mbFaceSpans);
            minibufferSnapshot = new RenderSnapshot.MinibufferSnapshot(
                    Optional.of(text), minibufferWindow.getDisplayStartColumn(), mbSpansOpt, mbTabWidth);
        } else if (messageBuffer.isShowingMessage()) {
            var msg = messageBuffer.getLastMessage();
            minibufferSnapshot = new RenderSnapshot.MinibufferSnapshot(msg, 0, Optional.empty(), defaultTabWidth);
        } else {
            minibufferSnapshot =
                    new RenderSnapshot.MinibufferSnapshot(Optional.empty(), 0, Optional.empty(), defaultTabWidth);
        }

        var cursorPosition = computeActiveCursorPosition(frame, layoutResult, minibufferRow, cols);

        return new RenderSnapshot(
                cols, rows, windowSnapshots, layoutResult.separators(), minibufferSnapshot, cursorPosition);
    }

    /**
     * アクティブウィンドウに応じたカーソル位置を計算する。
     * アクティブウィンドウがミニバッファウィンドウであればミニバッファ行に、
     * そうでなければレイアウト上のウィンドウ矩形内にカーソルを配置する。
     */
    static CursorPosition computeActiveCursorPosition(
            Frame frame, LayoutResult layoutResult, int minibufferRow, int cols) {
        var activeWindow = frame.getActiveWindow();
        if (activeWindow == frame.getMinibufferWindow()) {
            int tabWidth = activeWindow.getBuffer().getSettings().get(EditorSettings.TAB_WIDTH);
            return computeMinibufferCursorPosition(activeWindow, minibufferRow, cols, tabWidth);
        }
        var activeRect = layoutResult.windowRects().get(activeWindow);
        if (activeRect != null && activeRect.height() >= 2) {
            int tabWidth = activeWindow.getBuffer().getSettings().get(EditorSettings.TAB_WIDTH);
            return computeCursorPosition(activeWindow, activeRect, tabWidth);
        }
        return new CursorPosition(0, 0);
    }

    /**
     * 切り詰めモード時の可視行を構築する。
     */
    private static void buildTruncatedVisibleLines(
            BufferFacade buffer,
            int displayStart,
            int bufferRows,
            Optional<Integer> regionStart,
            Optional<Integer> regionEnd,
            MutableList<RenderSnapshot.LineSnapshot> visibleLines) {
        int lineCount = buffer.lineCount();
        var stylerOpt = buffer.getMajorMode().styler();
        if (stylerOpt.isPresent()) {
            SyntaxStyler styler = stylerOpt.get();
            MutableList<String> allLines = Lists.mutable.withInitialCapacity(lineCount);
            for (int i = 0; i < lineCount; i++) {
                allLines.add(buffer.lineText(i));
            }
            var allSpans = styler.styleDocument(allLines);
            int lineStart = buffer.lineStartOffset(displayStart);
            for (int row = 0; row < bufferRows; row++) {
                int lineIndex = displayStart + row;
                if (lineIndex < lineCount) {
                    String lineText = allLines.get(lineIndex);
                    int cpCount = (int) lineText.codePoints().count();
                    var spans = allSpans.get(lineIndex);
                    var merged = mergeWithTextPropertyFace(buffer, lineStart, cpCount, spans);
                    var lineRegion = computeLineRegion(lineStart, cpCount, regionStart, regionEnd);
                    visibleLines.add(RenderSnapshot.LineSnapshot.truncated(lineText, Optional.of(merged), lineRegion));
                    lineStart += cpCount + 1;
                }
            }
        } else {
            int lineStart = buffer.lineStartOffset(displayStart);
            for (int row = 0; row < bufferRows; row++) {
                int lineIndex = displayStart + row;
                if (lineIndex < lineCount) {
                    String lineText = buffer.lineText(lineIndex);
                    int cpCount = (int) lineText.codePoints().count();
                    var faceSpans = getTextPropertyFaceSpansForLine(buffer, lineStart, cpCount);
                    var spansOpt = faceSpans.isEmpty()
                            ? Optional.<ListIterable<StyledSpan>>empty()
                            : Optional.<ListIterable<StyledSpan>>of(faceSpans);
                    var lineRegion = computeLineRegion(lineStart, cpCount, regionStart, regionEnd);
                    visibleLines.add(RenderSnapshot.LineSnapshot.truncated(lineText, spansOpt, lineRegion));
                    lineStart += cpCount + 1;
                }
            }
        }
    }

    /**
     * 折り返しモード時の可視行を構築する。
     * 1バッファ行を表示幅で分割し、複数の視覚行として展開する。
     *
     * @param displayStartVl 表示開始バッファ行内の開始視覚行番号（長大行対応）
     */
    private static void buildWrappedVisibleLines(
            BufferFacade buffer,
            int displayStart,
            int displayStartVl,
            int bufferRows,
            int columns,
            Optional<Integer> regionStart,
            Optional<Integer> regionEnd,
            int tabWidth,
            MutableList<RenderSnapshot.LineSnapshot> visibleLines) {
        int lineCount = buffer.lineCount();
        var stylerOpt = buffer.getMajorMode().styler();

        // スタイラーの事前計算
        MutableList<String> allLines = null;
        ListIterable<ListIterable<StyledSpan>> allSpans = null;
        if (stylerOpt.isPresent()) {
            SyntaxStyler styler = stylerOpt.get();
            allLines = Lists.mutable.withInitialCapacity(lineCount);
            for (int i = 0; i < lineCount; i++) {
                allLines.add(buffer.lineText(i));
            }
            allSpans = styler.styleDocument(allLines);
        }

        int visualRowCount = 0;
        int lineStart = buffer.lineStartOffset(displayStart);
        for (int lineIndex = displayStart; lineIndex < lineCount && visualRowCount < bufferRows; lineIndex++) {
            String lineText = allLines != null ? allLines.get(lineIndex) : buffer.lineText(lineIndex);
            int cpCount = (int) lineText.codePoints().count();
            var breaks = VisualLineUtil.computeVisualLineBreaks(lineText, columns, tabWidth);
            int visualLineCount = breaks.size() + 1;

            Optional<ListIterable<StyledSpan>> spansOpt;
            if (allSpans != null) {
                var merged = mergeWithTextPropertyFace(buffer, lineStart, cpCount, allSpans.get(lineIndex));
                spansOpt = Optional.of(merged);
            } else {
                var faceSpans = getTextPropertyFaceSpansForLine(buffer, lineStart, cpCount);
                spansOpt = faceSpans.isEmpty()
                        ? Optional.<ListIterable<StyledSpan>>empty()
                        : Optional.<ListIterable<StyledSpan>>of(faceSpans);
            }

            var lineRegion = computeLineRegion(lineStart, cpCount, regionStart, regionEnd);

            // 表示開始行の場合はdisplayStartVl分だけ先頭の視覚行をスキップ
            int startVl = (lineIndex == displayStart) ? displayStartVl : 0;
            for (int vl = startVl; vl < visualLineCount && visualRowCount < bufferRows; vl++) {
                int startCp = vl == 0 ? 0 : breaks.get(vl - 1);
                int endCp = vl < breaks.size() ? breaks.get(vl) : cpCount;
                visibleLines.add(RenderSnapshot.LineSnapshot.wrapped(lineText, spansOpt, lineRegion, startCp, endCp));
                visualRowCount++;
            }
            // 次の行の開始オフセット = 現在行の開始 + コードポイント数 + 改行文字(1)
            lineStart += cpCount + 1;
        }
    }

    /**
     * 折り返しモード時のensurePointVisible。
     * 視覚行数を考慮してdisplayStartLine/displayStartVisualLineを調整する。
     */
    private static void ensurePointVisibleWrapped(Window window, int visibleRows, int columns, int tabWidth) {
        if (visibleRows <= 0) {
            return;
        }
        var buffer = window.getBuffer();
        int currentLine = buffer.lineIndexForOffset(window.getPoint());
        int displayStart = window.getDisplayStartLine();
        int displayStartVl = window.getDisplayStartVisualLine();

        // カーソルが表示開始行より前にある場合
        if (currentLine < displayStart) {
            window.setDisplayStartLine(currentLine);
            return;
        }

        // カーソル行までの視覚行数を計算（displayStartVisualLineを考慮）
        int visualRowsUsed = 0;
        for (int i = displayStart; i <= currentLine && i < buffer.lineCount(); i++) {
            String lineText = buffer.lineText(i);
            int vlCount = VisualLineUtil.computeVisualLineCount(lineText, columns, tabWidth);
            if (i == displayStart) {
                // 表示開始行は displayStartVl 分だけスキップ
                vlCount -= displayStartVl;
            }
            if (i == currentLine) {
                int lineStart = buffer.lineStartOffset(i);
                int cpOffset = window.getPoint() - lineStart;
                int cursorVisualLine = VisualLineUtil.computeVisualLineForOffset(lineText, columns, cpOffset, tabWidth);
                int effectiveCursorVl = (i == displayStart) ? cursorVisualLine - displayStartVl : cursorVisualLine;
                visualRowsUsed += effectiveCursorVl + 1;
            } else {
                visualRowsUsed += vlCount;
            }
        }

        // 表示範囲を超えている場合、displayStartLineを進める
        while (visualRowsUsed > visibleRows && displayStart < currentLine) {
            String lineText = buffer.lineText(displayStart);
            int vlCount = VisualLineUtil.computeVisualLineCount(lineText, columns, tabWidth) - displayStartVl;
            visualRowsUsed -= vlCount;
            displayStart++;
            displayStartVl = 0;
        }

        // それでも収まらない場合（1行が画面全体を超える長大行）
        // displayStartLine内の視覚行を進めてカーソルが見えるようにする
        if (visualRowsUsed > visibleRows && displayStart == currentLine) {
            String lineText = buffer.lineText(currentLine);
            int lineStart = buffer.lineStartOffset(currentLine);
            int cpOffset = window.getPoint() - lineStart;
            int cursorVisualLine = VisualLineUtil.computeVisualLineForOffset(lineText, columns, cpOffset, tabWidth);
            // カーソルの視覚行が画面末尾に来るようにdisplayStartVisualLineを設定
            displayStartVl = cursorVisualLine - visibleRows + 1;
            if (displayStartVl < 0) {
                displayStartVl = 0;
            }
        }

        if (displayStart != window.getDisplayStartLine()) {
            window.setDisplayStartLine(displayStart);
        }
        window.setDisplayStartVisualLine(displayStartVl);
    }

    /**
     * 折り返しモード時のハイライト行（visibleLines内の相対インデックス）を計算する。
     * カーソルのある視覚行のインデックスを返す。
     */
    private static OptionalInt computeWrappedHighlightLine(
            Window window, int displayStart, int pointLine, int columns, int visibleLinesSize, int tabWidth) {
        var buffer = window.getBuffer();
        int displayStartVl = window.getDisplayStartVisualLine();
        int visualRow = 0;
        for (int i = displayStart; i < pointLine && i < buffer.lineCount(); i++) {
            String lineText = buffer.lineText(i);
            int vlCount = VisualLineUtil.computeVisualLineCount(lineText, columns, tabWidth);
            if (i == displayStart) {
                vlCount -= displayStartVl;
            }
            visualRow += vlCount;
        }
        if (pointLine < buffer.lineCount()) {
            String lineText = buffer.lineText(pointLine);
            int lineStart = buffer.lineStartOffset(pointLine);
            int cpOffset = window.getPoint() - lineStart;
            int cursorVisualLine = VisualLineUtil.computeVisualLineForOffset(lineText, columns, cpOffset, tabWidth);
            if (pointLine == displayStart) {
                visualRow += cursorVisualLine - displayStartVl;
            } else {
                visualRow += cursorVisualLine;
            }
            if (visualRow >= 0 && visualRow < visibleLinesSize) {
                return OptionalInt.of(visualRow);
            }
        }
        return OptionalInt.empty();
    }

    private static String buildModeLineText(Window window, @Nullable StatusLineRenderer statusLineRenderer) {
        if (statusLineRenderer != null) {
            var buffer = window.getBuffer();
            var context = new StatusLineContext(window, buffer);
            var format = buffer.getSettings().get(StatusLineSettings.FORMAT);
            return statusLineRenderer.render(format.entries(), context);
        }
        return buildModeLineTextFallback(window);
    }

    private static String buildModeLineTextFallback(Window window) {
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int column = point - lineStart;

        String dirty = buffer.isDirty() ? "**" : "--";
        String truncate = window.isTruncateLines() ? "$" : "\\";
        String bufferName = buffer.getName();
        String modeName = buffer.getMajorMode().name();
        var minorModes = buffer.getMinorModes();
        String minorModesText = minorModes.isEmpty()
                ? ""
                : " " + minorModes.collect(m -> m.name()).makeString(" ");
        return String.format(
                "--%s%s  %s    (%d,%d)  (%s%s)",
                dirty, truncate, bufferName, lineIndex + 1, column, modeName, minorModesText);
    }

    private static CursorPosition computeCursorPosition(Window window, Rect rect, int tabWidth) {
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int displayStart = window.getDisplayStartLine();
        int bufferRows = rect.height() - 1;

        if (window.isTruncateLines()) {
            int screenRow = lineIndex - displayStart;
            if (screenRow < 0 || screenRow >= bufferRows) {
                return new CursorPosition(rect.left(), rect.top());
            }
            int col = DisplayWidthUtil.computeColumnForOffset(buffer.lineText(lineIndex), point - lineStart, tabWidth);
            int screenCol = col - window.getDisplayStartColumn();
            if (screenCol >= 0 && screenCol < rect.width()) {
                return new CursorPosition(rect.left() + screenCol, rect.top() + screenRow);
            }
            return new CursorPosition(rect.left(), rect.top());
        }

        // 折り返しモード: 視覚行を考慮したカーソル位置計算
        int columns = rect.width();
        int displayStartVl = window.getDisplayStartVisualLine();
        int visualRow = 0;
        for (int i = displayStart; i < lineIndex && i < buffer.lineCount(); i++) {
            String lineText = buffer.lineText(i);
            int vlCount = VisualLineUtil.computeVisualLineCount(lineText, columns, tabWidth);
            if (i == displayStart) {
                vlCount -= displayStartVl;
            }
            visualRow += vlCount;
        }
        String lineText = buffer.lineText(lineIndex);
        int cpOffset = point - lineStart;
        int cursorVisualLine = VisualLineUtil.computeVisualLineForOffset(lineText, columns, cpOffset, tabWidth);
        if (lineIndex == displayStart) {
            visualRow += cursorVisualLine - displayStartVl;
        } else {
            visualRow += cursorVisualLine;
        }

        if (visualRow < 0 || visualRow >= bufferRows) {
            return new CursorPosition(rect.left(), rect.top());
        }

        // 視覚行内でのカラム位置（視覚行ローカル基準）
        int visualLineStart = VisualLineUtil.visualLineStartOffset(lineText, columns, cursorVisualLine, tabWidth);
        int col = DisplayWidthUtil.computeColumnWidthInRange(lineText, visualLineStart, cpOffset, tabWidth);
        if (col >= 0 && col < columns) {
            return new CursorPosition(rect.left() + col, rect.top() + visualRow);
        }
        return new CursorPosition(rect.left(), rect.top());
    }

    /**
     * 指定行内のリージョン範囲（コードポイント単位の行ローカルオフセット）を計算する。
     * リージョンが行と重ならない場合はemptyを返す。
     *
     * @param lineStart 行のバッファ内コードポイントオフセット（事前計算済み）
     * @param cpCount 行のコードポイント数（事前計算済み）
     */
    private static Optional<RenderSnapshot.LineRegion> computeLineRegion(
            int lineStart, int cpCount, Optional<Integer> regionStart, Optional<Integer> regionEnd) {
        if (regionStart.isEmpty() || regionEnd.isEmpty()) {
            return Optional.empty();
        }
        int rStart = regionStart.get();
        int rEnd = regionEnd.get();
        if (rStart == rEnd) {
            return Optional.empty();
        }
        int lineEnd = lineStart + cpCount;

        // リージョンと行の交差を計算
        int overlapStart = Math.max(rStart, lineStart);
        int overlapEnd = Math.min(rEnd, lineEnd);
        if (overlapStart >= overlapEnd) {
            return Optional.empty();
        }
        return Optional.of(new RenderSnapshot.LineRegion(overlapStart - lineStart, overlapEnd - lineStart));
    }

    /**
     * テキストプロパティのfaceSpansを行ローカル座標に変換して取得する。
     *
     * @param lineStart 行のバッファ内コードポイントオフセット（事前計算済み）
     * @param cpCount 行のコードポイント数（事前計算済み）
     */
    private static ListIterable<StyledSpan> getTextPropertyFaceSpansForLine(
            BufferFacade buffer, int lineStart, int cpCount) {
        int lineEnd = lineStart + cpCount;
        var bufferSpans = buffer.getFaceSpans(lineStart, lineEnd);
        if (bufferSpans.isEmpty()) {
            return bufferSpans;
        }
        // バッファオフセットから行ローカル座標に変換
        MutableList<StyledSpan> localSpans = Lists.mutable.withInitialCapacity(bufferSpans.size());
        for (var span : bufferSpans) {
            localSpans.add(new StyledSpan(span.start() - lineStart, span.end() - lineStart, span.faceName()));
        }
        return localSpans;
    }

    /**
     * SyntaxStylerのスパンとテキストプロパティのfaceスパンをマージする。
     * テキストプロパティfaceが優先（効果範囲の狭いものが強い）。
     *
     * <p>両方のスパンリストはstart順にソート済みの前提。
     * SyntaxStylerスパンの隙間やスパン外にあるfaceスパンも正しく出力する。
     */
    private static ListIterable<StyledSpan> mergeWithTextPropertyFace(
            BufferFacade buffer, int lineStart, int cpCount, ListIterable<StyledSpan> stylerSpans) {
        var faceSpans = getTextPropertyFaceSpansForLine(buffer, lineStart, cpCount);
        if (faceSpans.isEmpty()) {
            return stylerSpans;
        }
        MutableList<StyledSpan> result = Lists.mutable.empty();
        int fIdx = 0;
        int stylerIdx = 0;

        // SyntaxStylerスパンの前にあるfaceスパンを追加
        while (fIdx < faceSpans.size()
                && (stylerIdx >= stylerSpans.size()
                        || faceSpans.get(fIdx).end()
                                <= stylerSpans.get(stylerIdx).start())) {
            if (stylerIdx < stylerSpans.size()) {
                int fEnd = Math.min(
                        faceSpans.get(fIdx).end(), stylerSpans.get(stylerIdx).start());
                if (faceSpans.get(fIdx).start() < fEnd) {
                    result.add(new StyledSpan(
                            faceSpans.get(fIdx).start(),
                            fEnd,
                            faceSpans.get(fIdx).faceName()));
                }
                if (faceSpans.get(fIdx).end() <= stylerSpans.get(stylerIdx).start()) {
                    fIdx++;
                } else {
                    break;
                }
            } else {
                result.add(faceSpans.get(fIdx));
                fIdx++;
            }
        }

        for (; stylerIdx < stylerSpans.size(); stylerIdx++) {
            var stylerSpan = stylerSpans.get(stylerIdx);
            int sStart = stylerSpan.start();
            int sEnd = stylerSpan.end();

            // このSyntaxStylerスパンの前にあるfaceスパン（スパン間の隙間）
            while (fIdx < faceSpans.size() && faceSpans.get(fIdx).end() <= sStart) {
                result.add(faceSpans.get(fIdx));
                fIdx++;
            }

            // SyntaxStylerスパンとfaceスパンのオーバーラップ処理
            int cursor = sStart;
            int tempIdx = fIdx;
            while (tempIdx < faceSpans.size() && faceSpans.get(tempIdx).start() < sEnd) {
                var fSpan = faceSpans.get(tempIdx);
                int fClampedStart = Math.max(fSpan.start(), sStart);
                int fClampedEnd = Math.min(fSpan.end(), sEnd);
                if (cursor < fClampedStart) {
                    result.add(new StyledSpan(cursor, fClampedStart, stylerSpan.faceName()));
                }
                result.add(new StyledSpan(fClampedStart, fClampedEnd, fSpan.faceName()));
                cursor = fClampedEnd;
                tempIdx++;
            }
            if (cursor < sEnd) {
                result.add(new StyledSpan(cursor, sEnd, stylerSpan.faceName()));
            }

            // fIdxを進める（完全にこのstylerSpan内に収まったfaceSpanをスキップ）
            while (fIdx < faceSpans.size() && faceSpans.get(fIdx).end() <= sEnd) {
                fIdx++;
            }
        }

        // SyntaxStylerスパンの後に残ったfaceスパンを追加
        while (fIdx < faceSpans.size()) {
            result.add(faceSpans.get(fIdx));
            fIdx++;
        }

        return result;
    }

    private static CursorPosition computeMinibufferCursorPosition(
            Window minibufferWindow, int row, int maxColumns, int tabWidth) {
        var buffer = minibufferWindow.getBuffer();
        int point = minibufferWindow.getPoint();
        String text = buffer.getText();
        int col = DisplayWidthUtil.computeColumnForOffset(text, point, tabWidth);
        int screenCol = col - minibufferWindow.getDisplayStartColumn();
        if (screenCol >= 0 && screenCol < maxColumns) {
            return new CursorPosition(screenCol, row);
        }
        return new CursorPosition(0, row);
    }
}
