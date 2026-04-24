package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import java.nio.file.Path;
import java.time.ZoneId;

/**
 * ディレクトリを Tree Dired バッファで開くサービス。
 * TreeDiredCommand や PathOpenService など、複数の箇所から利用される。
 */
public class DiredOpenService {

    private final DirectoryLister directoryLister;
    private final ZoneId zoneId;
    private final Keymap diredKeymap;
    private final CommandRegistry diredCommandRegistry;
    private final ModeRegistry modeRegistry;
    private final SettingsRegistry settingsRegistry;

    public DiredOpenService(
            DirectoryLister directoryLister,
            ZoneId zoneId,
            Keymap diredKeymap,
            CommandRegistry diredCommandRegistry,
            ModeRegistry modeRegistry,
            SettingsRegistry settingsRegistry) {
        this.directoryLister = directoryLister;
        this.zoneId = zoneId;
        this.diredKeymap = diredKeymap;
        this.diredCommandRegistry = diredCommandRegistry;
        this.modeRegistry = modeRegistry;
        this.settingsRegistry = settingsRegistry;
    }

    /**
     * 指定パスのディレクトリを Tree Dired バッファで開く。
     * 同名バッファが既にある場合はそのバッファに切り替える。
     *
     * @param pathString ディレクトリパス文字列
     * @param bufferManager バッファマネージャ
     * @param frame フレーム
     */
    public void openDired(String pathString, BufferManager bufferManager, Frame frame) {
        if (pathString.isEmpty()) {
            return;
        }
        var path = Path.of(pathString).toAbsolutePath().normalize();

        String bufferName = "*Dired " + path + "*";

        // 同名バッファが既にある場合はそこに切り替え
        var existing = bufferManager.findByName(bufferName);
        if (existing.isPresent()) {
            frame.getActiveWindow().setBuffer(existing.get());
            return;
        }

        // バッファ作成（ディレクトリパスをfilePathに設定し、find-file等の起点にする）
        var textBuffer = new TextBuffer(bufferName, new GapTextModel(), settingsRegistry, path);
        var bufferFacade = new BufferFacade(textBuffer);

        // モデル・モード作成
        var model = new TreeDiredModel(path, directoryLister);
        var mode = new TreeDiredMode(model, diredKeymap, zoneId, diredCommandRegistry);
        bufferFacade.setMajorMode(mode);
        mode.onEnable(bufferFacade);
        modeRegistry.runMajorModeHooks(mode.name(), bufferFacade);

        // バッファを登録してウィンドウに表示
        bufferManager.add(bufferFacade);
        frame.getActiveWindow().setBuffer(bufferFacade);

        // 初期レンダリング（read-onlyは設定前に行う）
        var customColumns = TreeDiredBufferUpdater.resolveCustomColumns(bufferFacade);
        var rootSuffix = TreeDiredBufferUpdater.resolveRootSuffix(bufferFacade);
        TreeDiredRenderer.render(
                bufferFacade, model.getRootDirectory(), model.getVisibleEntries(), zoneId, customColumns, rootSuffix);
        bufferFacade.setReadOnly(true);
        bufferFacade.markClean();
        frame.getActiveWindow().setPoint(0);
    }
}
