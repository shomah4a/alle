package io.github.shomah4a.alle.core.io;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.mode.AutoModeMap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ファイルを開くサービス。
 * パス文字列からバッファを生成し、BufferManagerに登録してアクティブウィンドウに表示する。
 * FindFileCommandや起動時のファイルオープンなど、複数の箇所から利用される。
 */
public class FileOpenService {

    private static final Logger logger = Logger.getLogger(FileOpenService.class.getName());

    private final BufferIO bufferIO;
    private final AutoModeMap autoModeMap;
    private final ModeRegistry modeRegistry;
    private final SettingsRegistry settingsRegistry;

    public FileOpenService(
            BufferIO bufferIO, AutoModeMap autoModeMap, ModeRegistry modeRegistry, SettingsRegistry settingsRegistry) {
        this.bufferIO = bufferIO;
        this.autoModeMap = autoModeMap;
        this.modeRegistry = modeRegistry;
        this.settingsRegistry = settingsRegistry;
    }

    /**
     * 指定パスのファイルを開く。
     * 同一パスのバッファが既に存在する場合はそのバッファに切り替える。
     * ファイルが存在しない場合は空バッファをファイルパス付きで作成する。
     * メジャーモードを自動設定し、モードフックを実行する。
     *
     * @param pathString ファイルパス文字列（末尾スラッシュは除去される）
     * @param bufferManager バッファマネージャ
     * @param frame フレーム（アクティブウィンドウにバッファを設定するため）
     */
    public void openFile(String pathString, BufferManager bufferManager, Frame frame) {
        if (pathString.isEmpty()) {
            return;
        }
        String trimmed = pathString.endsWith("/") ? pathString.substring(0, pathString.length() - 1) : pathString;
        var path = normalizePath(trimmed);

        // 同一パスのバッファが既に存在する場合はそのバッファに切り替え
        var existingBuffer = bufferManager.findByPath(path);
        if (existingBuffer.isPresent()) {
            frame.getActiveWindow().setBuffer(existingBuffer.get());
            return;
        }

        // ファイルを読み込むか、存在しなければ空バッファを作成
        BufferFacade bufferFacade;
        try {
            var loadResult = bufferIO.load(path);
            bufferFacade = loadResult.bufferFacade();
        } catch (IOException e) {
            logger.log(Level.FINE, "ファイルが存在しないため空バッファを作成: " + path, e);
            bufferFacade = new BufferFacade(
                    new TextBuffer(path.getFileName().toString(), new GapTextModel(), settingsRegistry, path));
        }

        var targetBuffer = bufferFacade;
        var majorMode = autoModeMap.resolve(bufferFacade.getName(), () -> targetBuffer.lineText(0));
        bufferFacade.setMajorMode(majorMode);
        modeRegistry.runMajorModeHooks(majorMode.name(), bufferFacade);
        bufferManager.add(bufferFacade);
        frame.getActiveWindow().setBuffer(bufferFacade);
    }

    /**
     * パス文字列を絶対パスに変換し正規化する。
     */
    public static Path normalizePath(String pathString) {
        return Path.of(pathString).toAbsolutePath().normalize();
    }
}
