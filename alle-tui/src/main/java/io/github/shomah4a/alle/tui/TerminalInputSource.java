package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.input.InputSource;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * LanternaのScreenからキー入力を読み取るInputSource実装。
 * shutdownフラグによる安全な終了をサポートする。
 */
public class TerminalInputSource implements InputSource {

    private final Screen screen;
    private volatile boolean shutdownRequested;

    public TerminalInputSource(Screen screen) {
        this.screen = screen;
        this.shutdownRequested = false;
    }

    /**
     * 終了を要求する。次のreadKeyStroke呼び出しでemptyを返す。
     */
    public void requestShutdown() {
        this.shutdownRequested = true;
    }

    @Override
    public Optional<KeyStroke> readKeyStroke() {
        while (!shutdownRequested) {
            try {
                var lanternaKeyStroke = screen.readInput();
                if (lanternaKeyStroke == null
                        || lanternaKeyStroke.getKeyType() == com.googlecode.lanterna.input.KeyType.EOF) {
                    return Optional.empty();
                }
                var converted = KeyStrokeConverter.convert(lanternaKeyStroke);
                if (converted.isPresent()) {
                    return converted;
                }
                // 未対応キーはスキップして次のキーを読む
            } catch (IOException e) {
                throw new UncheckedIOException("キー入力の読み取りに失敗しました", e);
            }
        }
        return Optional.empty();
    }
}
