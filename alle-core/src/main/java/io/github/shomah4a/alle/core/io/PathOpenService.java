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
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * パスを開くサービス。
 * ファイルの場合はバッファを生成してアクティブウィンドウに表示し、
 * ディレクトリの場合はDirectoryOpenerに委譲する。
 * FindFileCommand、TreeDiredFindFileOrToggleCommand、起動時のファイルオープンなど、
 * 複数の箇所から利用される。
 */
public class PathOpenService {

    /**
     * ディレクトリを開く処理の委譲先。
     */
    public interface DirectoryOpener {
        void openDirectory(String pathString, BufferManager bufferManager, Frame frame);
    }

    private static final Logger logger = Logger.getLogger(PathOpenService.class.getName());

    private final BufferIO bufferIO;
    private final AutoModeMap autoModeMap;
    private final ModeRegistry modeRegistry;
    private final SettingsRegistry settingsRegistry;
    private final Predicate<Path> directoryChecker;
    private final DirectoryOpener directoryOpener;

    public PathOpenService(
            BufferIO bufferIO,
            AutoModeMap autoModeMap,
            ModeRegistry modeRegistry,
            SettingsRegistry settingsRegistry,
            Predicate<Path> directoryChecker,
            DirectoryOpener directoryOpener) {
        this.bufferIO = bufferIO;
        this.autoModeMap = autoModeMap;
        this.modeRegistry = modeRegistry;
        this.settingsRegistry = settingsRegistry;
        this.directoryChecker = directoryChecker;
        this.directoryOpener = directoryOpener;
    }

    /**
     * 指定パスを開く。
     * ディレクトリの場合はDiredで開き、ファイルの場合はバッファに読み込む。
     * 同一パスのバッファが既に存在する場合はそのバッファに切り替える。
     * ファイルが存在しない場合は空バッファをファイルパス付きで作成する。
     * メジャーモードを自動設定し、モードフックを実行する。
     *
     * @param pathString パス文字列（末尾スラッシュは除去される）
     * @param bufferManager バッファマネージャ
     * @param frame フレーム（アクティブウィンドウにバッファを設定するため）
     */
    public void open(String pathString, BufferManager bufferManager, Frame frame) {
        if (pathString.isEmpty()) {
            return;
        }
        var path = normalizePath(pathString);

        // ディレクトリの場合はDiredで開く
        if (directoryChecker.test(path)) {
            directoryOpener.openDirectory(path.toString(), bufferManager, frame);
            return;
        }

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
