package io.github.shomah4a.alle.core.render;

import io.github.shomah4a.alle.core.DisplayWidthUtil;
import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.styling.StyledSpan;
import io.github.shomah4a.alle.core.styling.StylingState;
import io.github.shomah4a.alle.core.styling.SyntaxStyler;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.LayoutResult;
import io.github.shomah4a.alle.core.window.Rect;
import io.github.shomah4a.alle.core.window.Window;
import io.github.shomah4a.alle.core.window.WindowLayout;
import java.util.Optional;
import java.util.OptionalInt;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

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
            window.ensurePointVisible(bufferRows);
            window.ensurePointHorizontallyVisible(rect.width());

            var buffer = window.getBuffer();
            int lineCount = buffer.lineCount();
            int displayStart = window.getDisplayStartLine();
            int displayStartColumn = window.getDisplayStartColumn();
            var stylerOpt = buffer.getMajorMode().styler();

            var visibleLines = Lists.mutable.<RenderSnapshot.LineSnapshot>empty();
            if (stylerOpt.isPresent()) {
                SyntaxStyler styler = stylerOpt.get();
                StylingState styleState = styler.initialState();
                for (int i = 0; i < displayStart && i < lineCount; i++) {
                    styleState = styler.styleLineWithState(buffer.lineText(i), styleState)
                            .nextState();
                }
                for (int row = 0; row < bufferRows; row++) {
                    int lineIndex = displayStart + row;
                    if (lineIndex < lineCount) {
                        String lineText = buffer.lineText(lineIndex);
                        var result = styler.styleLineWithState(lineText, styleState);
                        var merged = mergeWithTextPropertyFace(buffer, lineIndex, result.spans());
                        visibleLines.add(new RenderSnapshot.LineSnapshot(lineText, Optional.of(merged)));
                        styleState = result.nextState();
                    }
                }
            } else {
                for (int row = 0; row < bufferRows; row++) {
                    int lineIndex = displayStart + row;
                    if (lineIndex < lineCount) {
                        String lineText = buffer.lineText(lineIndex);
                        var faceSpans = getTextPropertyFaceSpansForLine(buffer, lineIndex);
                        var spansOpt = faceSpans.isEmpty()
                                ? Optional.<ListIterable<StyledSpan>>empty()
                                : Optional.<ListIterable<StyledSpan>>of(faceSpans);
                        visibleLines.add(new RenderSnapshot.LineSnapshot(lineText, spansOpt));
                    }
                }
            }

            String modeLine = buildModeLineText(window);
            OptionalInt highlightLine = OptionalInt.empty();
            if (window.isHighlightPointLine()) {
                int pointLine = buffer.lineIndexForOffset(window.getPoint());
                int relativeLine = pointLine - displayStart;
                if (relativeLine >= 0 && relativeLine < visibleLines.size()) {
                    highlightLine = OptionalInt.of(relativeLine);
                }
            }
            windowSnapshots.add(
                    new RenderSnapshot.WindowSnapshot(rect, visibleLines, displayStartColumn, modeLine, highlightLine));
        });

        // ミニバッファ / エコーエリア
        RenderSnapshot.MinibufferSnapshot minibufferSnapshot;
        if (frame.isMinibufferActive()) {
            var minibufferWindow = frame.getMinibufferWindow();
            minibufferWindow.ensurePointHorizontallyVisible(cols);
            var mbBuffer = minibufferWindow.getBuffer();
            String text = mbBuffer.length() > 0 ? mbBuffer.getText() : "";
            var mbFaceSpans = mbBuffer.getFaceSpans(0, mbBuffer.length());
            var mbSpansOpt = mbFaceSpans.isEmpty()
                    ? Optional.<ListIterable<StyledSpan>>empty()
                    : Optional.<ListIterable<StyledSpan>>of(mbFaceSpans);
            minibufferSnapshot = new RenderSnapshot.MinibufferSnapshot(
                    Optional.of(text), minibufferWindow.getDisplayStartColumn(), mbSpansOpt);
        } else if (messageBuffer.isShowingMessage()) {
            var msg = messageBuffer.getLastMessage();
            minibufferSnapshot = new RenderSnapshot.MinibufferSnapshot(msg, 0, Optional.empty());
        } else {
            minibufferSnapshot = new RenderSnapshot.MinibufferSnapshot(Optional.empty(), 0, Optional.empty());
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
            return computeMinibufferCursorPosition(activeWindow, minibufferRow, cols);
        }
        var activeRect = layoutResult.windowRects().get(activeWindow);
        if (activeRect != null && activeRect.height() >= 2) {
            return computeCursorPosition(activeWindow, activeRect);
        }
        return new CursorPosition(0, 0);
    }

    private static String buildModeLineText(Window window) {
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int column = point - lineStart;

        String dirty = buffer.isDirty() ? "**" : "--";
        String bufferName = buffer.getName();
        String modeName = buffer.getMajorMode().name();
        var minorModes = buffer.getMinorModes();
        String minorModesText = minorModes.isEmpty()
                ? ""
                : " " + minorModes.collect(m -> m.name()).makeString(" ");
        return String.format(
                "--%s  %s    (%d,%d)  (%s%s)", dirty, bufferName, lineIndex + 1, column, modeName, minorModesText);
    }

    private static CursorPosition computeCursorPosition(Window window, Rect rect) {
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int displayStart = window.getDisplayStartLine();
        int bufferRows = rect.height() - 1;

        int screenRow = lineIndex - displayStart;
        if (screenRow < 0 || screenRow >= bufferRows) {
            return new CursorPosition(rect.left(), rect.top());
        }

        int col = DisplayWidthUtil.computeColumnForOffset(buffer.lineText(lineIndex), point - lineStart);
        int screenCol = col - window.getDisplayStartColumn();
        if (screenCol >= 0 && screenCol < rect.width()) {
            return new CursorPosition(rect.left() + screenCol, rect.top() + screenRow);
        }
        return new CursorPosition(rect.left(), rect.top());
    }

    /**
     * テキストプロパティのfaceSpansを行ローカル座標に変換して取得する。
     */
    private static ListIterable<StyledSpan> getTextPropertyFaceSpansForLine(BufferFacade buffer, int lineIndex) {
        int lineStart = buffer.lineStartOffset(lineIndex);
        int lineLength = (int) buffer.lineText(lineIndex).codePoints().count();
        int lineEnd = lineStart + lineLength;
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
            BufferFacade buffer, int lineIndex, ListIterable<StyledSpan> stylerSpans) {
        var faceSpans = getTextPropertyFaceSpansForLine(buffer, lineIndex);
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

    private static CursorPosition computeMinibufferCursorPosition(Window minibufferWindow, int row, int maxColumns) {
        var buffer = minibufferWindow.getBuffer();
        int point = minibufferWindow.getPoint();
        String text = buffer.getText();
        int col = DisplayWidthUtil.computeColumnForOffset(text, point);
        int screenCol = col - minibufferWindow.getDisplayStartColumn();
        if (screenCol >= 0 && screenCol < maxColumns) {
            return new CursorPosition(screenCol, row);
        }
        return new CursorPosition(0, row);
    }
}
