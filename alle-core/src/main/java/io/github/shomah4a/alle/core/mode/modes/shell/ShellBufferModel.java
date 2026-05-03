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
    private final InteractiveShellProcess process;
    private final AnsiParser ansiParser;
    private int inputStartPosition;
    private volatile boolean processFinished;

    ShellBufferModel(BufferFacade buffer, InteractiveShellProcess process) {
        this.buffer = buffer;
        this.process = process;
        this.ansiParser = new AnsiParser();
        this.inputStartPosition = 0;
        this.processFinished = false;
    }

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
            String userInput = getCurrentInput();

            // ユーザー入力を削除
            if (!userInput.isEmpty()) {
                buf.deleteText(inputStartPosition, userInput.length());
            }

            // ANSIパースしてテキスト挿入とFace適用
            ImmutableList<AnsiParser.StyledSegment> segments = ansiParser.parse(rawLine);
            int insertPos = inputStartPosition;
            for (var segment : segments) {
                buf.insertText(insertPos, segment.text());
                @Nullable FaceName face = segment.face();
                if (face != null) {
                    int segLen = segment.text().length();
                    buf.putFace(insertPos, insertPos + segLen, face);
                }
                insertPos += segment.text().length();
            }

            // 改行を追加
            buf.insertText(insertPos, "\n");
            insertPos += 1;

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
            if (inputStartPosition > 0) {
                buf.putReadOnly(0, inputStartPosition);
            }

            return currentInput;
        });

        process.sendInput(input);
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
