package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.input.InputSource;
import io.github.shomah4a.alle.core.input.ShutdownRequestable;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * LanternaのScreenからキー入力を読み取るInputSource実装。
 * shutdownフラグによる安全な終了をサポートする。
 */
public class TerminalInputSource implements InputSource, ShutdownRequestable, Loggable {

    private final Screen screen;
    private volatile boolean shutdownRequested;

    public TerminalInputSource(Screen screen) {
        this.screen = screen;
        this.shutdownRequested = false;
    }

    /**
     * 終了を要求する。次のreadKeyStroke呼び出しでemptyを返す。
     */
    @Override
    public void requestShutdown() {
        this.shutdownRequested = true;
    }

    @Override
    public Optional<KeyStroke> readKeyStroke() {
        while (!shutdownRequested) {
            try {
                var lanternaKeyStroke = screen.pollInput();
                if (lanternaKeyStroke == null) {
                    Thread.sleep(5);
                    continue;
                }
                logger().debug(
                                "lanterna: keyType={}, character={}, charCode={}, ctrl={}, alt={}, shift={}",
                                lanternaKeyStroke.getKeyType(),
                                lanternaKeyStroke.getCharacter(),
                                lanternaKeyStroke.getCharacter() != null
                                        ? Integer.toHexString(lanternaKeyStroke.getCharacter())
                                        : "null",
                                lanternaKeyStroke.isCtrlDown(),
                                lanternaKeyStroke.isAltDown(),
                                lanternaKeyStroke.isShiftDown());
                if (lanternaKeyStroke.getKeyType() == com.googlecode.lanterna.input.KeyType.EOF) {
                    return Optional.empty();
                }
                var converted = KeyStrokeConverter.convert(lanternaKeyStroke);
                logger().debug(
                                "converted: {}",
                                converted
                                        .map(k -> "keyCode=" + Integer.toHexString(k.keyCode()) + " modifiers="
                                                + k.modifiers())
                                        .orElse("empty"));
                if (converted.isPresent()) {
                    return converted;
                }
                // 未対応キーはスキップして次のキーを読む
            } catch (IOException e) {
                throw new UncheckedIOException("キー入力の読み取りに失敗しました", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
