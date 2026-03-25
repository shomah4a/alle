package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/**
 * コマンド実行時のコンテキスト。
 * 編集操作はactiveWindow経由で行う。
 * バッファの作成・削除・一覧取得はbufferManagerを通じて行う。
 * ユーザーからの文字列入力はinputPrompter経由で行う。
 * triggeringKeyはコマンドを発動したキーストローク（プログラム的呼び出し時はempty）。
 * thisCommandは現在実行中のコマンド名、lastCommandは直前に実行されたコマンド名。
 * killRingはkillコマンドで削除されたテキストの蓄積先。
 * messageBufferはエコーエリアへのメッセージ出力先。
 */
public record CommandContext(
        Frame frame,
        BufferManager bufferManager,
        Window activeWindow,
        InputPrompter inputPrompter,
        Optional<KeyStroke> triggeringKey,
        Optional<String> thisCommand,
        Optional<String> lastCommand,
        KillRing killRing,
        MessageBuffer messageBuffer,
        MessageBuffer warningBuffer,
        SettingsRegistry settingsRegistry) {

    /**
     * エラーをエコーエリアと*Warnings*バッファに通知する。
     * エコーエリアには短いメッセージを表示し、*Warnings*にはメッセージとスタックトレース全行を書き込む。
     */
    public void handleError(String message, Throwable ex) {
        messageBuffer.message(message);
        warningBuffer.message(message);
        for (String line : stackTraceToString(ex).lines().toList()) {
            warningBuffer.message(line);
        }
    }

    private static String stackTraceToString(Throwable ex) {
        var sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
