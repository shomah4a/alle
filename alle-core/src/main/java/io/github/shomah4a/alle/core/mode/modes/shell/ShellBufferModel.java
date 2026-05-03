package io.github.shomah4a.alle.core.mode.modes.shell;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.input.InteractiveShellProcess;
import io.github.shomah4a.alle.core.styling.FaceName;
import org.eclipse.collections.api.list.ImmutableList;
import org.jspecify.annotations.Nullable;

/**
 * シェルバッファの状態を管理するモデル。
 *
 * <p>ユーザー入力の開始位置 ({@code inputStartPosition}) を追跡し、
 * 出力領域を read-only で保護する。
 * プロセスからの出力到着時はユーザー入力を退避して出力を挿入し、入力を復元する。
 */
final class ShellBufferModel {

    private final BufferFacade buffer;
    private final AnsiParser ansiParser;
    private final Runnable onOutputAppended;
    private volatile @Nullable InteractiveShellProcess process;
    private int inputStartPosition;
    private volatile boolean processFinished;
    private boolean pendingNewline;

    ShellBufferModel(BufferFacade buffer, Runnable onOutputAppended) {
        this.buffer = buffer;
        this.ansiParser = new AnsiParser();
        this.onOutputAppended = onOutputAppended;
        this.inputStartPosition = 0;
        this.processFinished = false;
        this.pendingNewline = false;
    }

    void setProcess(InteractiveShellProcess process) {
        this.process = process;
    }

    @Nullable
    InteractiveShellProcess getProcess() {
        return process;
    }

    int getInputStartPosition() {
        return inputStartPosition;
    }

    boolean isProcessFinished() {
        return processFinished;
    }

    /**
     * 現在のユーザー入力テキストを返す。
     */
    String getCurrentInput() {
        int length = buffer.length();
        if (inputStartPosition >= length) {
            return "";
        }
        return buffer.substring(inputStartPosition, length);
    }

    /**
     * プロセスからの出力行をバッファに追記する。
     * ユーザー入力が存在する場合は退避→出力挿入→復元の手順で処理する。
     * バックグラウンドスレッドから呼ばれるため、{@code atomicOperation} 内で実行する。
     *
     * @param rawLine ANSIエスケープシーケンスを含む可能性のある出力行
     */
    void appendOutput(String rawLine) {
        buffer.atomicOperation(buf -> {
            // ANSIパースしてテキストとスタイルを取得
            ImmutableList<AnsiParser.StyledSegment> segments = ansiParser.parse(rawLine);

            // パース後のテキストが空（CRのみの行等）であればスキップ
            int totalLength = 0;
            for (var segment : segments) {
                totalLength += segment.text().length();
            }
            if (totalLength == 0) {
                return null;
            }

            String userInput = getCurrentInput();

            // ユーザー入力を削除
            if (!userInput.isEmpty()) {
                buf.deleteText(inputStartPosition, userInput.length());
            }

            int insertPos = inputStartPosition;

            // 前回の行の改行を遅延挿入（プロンプト行の後に改行を入れないため）
            if (pendingNewline) {
                buf.insertText(insertPos, "\n");
                insertPos += 1;
            }

            // テキスト挿入とFace適用
            for (var segment : segments) {
                buf.insertText(insertPos, segment.text());
                @Nullable FaceName face = segment.face();
                if (face != null) {
                    int segLen = segment.text().length();
                    buf.putFace(insertPos, insertPos + segLen, face);
                }
                insertPos += segment.text().length();
            }

            // 次の出力行が来たときに改行を挿入する
            pendingNewline = true;

            // inputStartPositionを更新
            inputStartPosition = insertPos;

            // 出力領域をread-onlyに設定
            if (inputStartPosition > 0) {
                buf.putReadOnly(0, inputStartPosition);
            }

            // ユーザー入力を復元
            if (!userInput.isEmpty()) {
                buf.insertText(inputStartPosition, userInput);
            }

            return null;
        });

        onOutputAppended.run();
    }

    /**
     * ユーザー入力をプロセスに送信し、入力領域をread-onlyにする。
     */
    void sendInput() {
        if (processFinished) {
            return;
        }

        String input = buffer.atomicOperation(buf -> {
            String currentInput = getCurrentInput();

            // 入力の後に改行を追加
            buf.insertText(buf.length(), "\n");

            // 入力完了した領域をread-onlyに設定
            inputStartPosition = buf.length();
            pendingNewline = false;
            if (inputStartPosition > 0) {
                buf.putReadOnly(0, inputStartPosition);
            }

            return currentInput;
        });

        var proc = this.process;
        if (proc != null) {
            proc.sendInput(input);
        }
    }

    /**
     * プロセス終了メッセージをバッファに追記する。
     */
    void markProcessFinished() {
        processFinished = true;
        appendFinishedMessage();
    }

    private void appendFinishedMessage() {
        buffer.atomicOperation(buf -> {
            String msg = "\nProcess shell finished\n";
            buf.insertText(buf.length(), msg);
            inputStartPosition = buf.length();
            buf.putReadOnly(0, inputStartPosition);
            return null;
        });
    }
}
