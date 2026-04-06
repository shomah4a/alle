package io.github.shomah4a.alle.script;

import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.constants.BufferNames;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.keybind.KeymapEntry;
import io.github.shomah4a.alle.core.mode.AutoModeMap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.syntax.SyntaxAnalyzerRegistry;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.FrameLayoutStore;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.graalvm.polyglot.Value;
import org.jspecify.annotations.Nullable;

/**
 * スクリプトに公開するエディタのルートファサード。
 * アクティブウィンドウ・バッファの解決、メッセージ表示、
 * コマンド登録・実行、キーバインド設定、モード登録を担う。
 */
public class EditorFacade implements Loggable {

    private final Frame frame;
    private final MessageBuffer messageBuffer;
    private final CommandRegistry commandRegistry;
    private final Keymap globalKeymap;
    private final ModeRegistry modeRegistry;
    private final AutoModeMap autoModeMap;
    private final SyntaxAnalyzerRegistry syntaxAnalyzerRegistry;
    private final FrameLayoutStore frameLayoutStore;
    private final BufferManager bufferManager;

    public EditorFacade(
            Frame frame,
            MessageBuffer messageBuffer,
            CommandRegistry commandRegistry,
            Keymap globalKeymap,
            ModeRegistry modeRegistry,
            AutoModeMap autoModeMap,
            SyntaxAnalyzerRegistry syntaxAnalyzerRegistry,
            FrameLayoutStore frameLayoutStore,
            BufferManager bufferManager) {
        this.frame = frame;
        this.messageBuffer = messageBuffer;
        this.commandRegistry = commandRegistry;
        this.globalKeymap = globalKeymap;
        this.modeRegistry = modeRegistry;
        this.autoModeMap = autoModeMap;
        this.syntaxAnalyzerRegistry = syntaxAnalyzerRegistry;
        this.frameLayoutStore = frameLayoutStore;
        this.bufferManager = bufferManager;
    }

    /**
     * アクティブウィンドウのファサードを返す。
     */
    public WindowFacade activeWindow() {
        return new WindowFacade(frame.getActiveWindow());
    }

    /**
     * アクティブウィンドウのバッファのファサードを返す。
     */
    public BufferFacade currentBuffer() {
        return new BufferFacade(frame.getActiveWindow().getBuffer());
    }

    /**
     * エコーエリアにメッセージを表示する。
     */
    public void message(String text) {
        messageBuffer.message(text);
    }

    /**
     * コマンドを登録する。同名のコマンドが既に存在する場合は上書きする。
     * Python側からAlleCommand（Java Commandを直接継承）が渡される。
     */
    public void registerCommand(Value command) {
        commandRegistry.registerOrReplace(command.as(Command.class));
    }

    /**
     * 登録済みコマンドを名前で検索する。
     *
     * @param name コマンド名
     * @return コマンドが見つかった場合はそのCommand、見つからない場合はnull
     */
    public @Nullable Command lookupCommand(String name) {
        return commandRegistry.lookup(name).orElse(null);
    }

    /**
     * グローバルキーマップにキーバインドを設定する。
     * キーストロークのリストが複数要素の場合、プレフィックスキーを自動解決する。
     *
     * @param keyStrokes キーストロークのリスト（例: [ctrl('x'), ctrl('f')]）
     * @param commandValue バインドするコマンド
     */
    public void globalSetKey(List<KeyStroke> keyStrokes, Value commandValue) {
        Command command = commandValue.as(Command.class);
        if (keyStrokes.isEmpty()) {
            throw new IllegalArgumentException("キーストロークのリストが空です");
        }
        if (keyStrokes.size() == 1) {
            globalKeymap.bind(keyStrokes.get(0), command);
            return;
        }
        // プレフィックスキーの自動解決
        Keymap current = globalKeymap;
        for (int i = 0; i < keyStrokes.size() - 1; i++) {
            KeyStroke prefix = keyStrokes.get(i);
            Optional<KeymapEntry> entry = current.lookup(prefix);
            if (entry.isPresent() && entry.get() instanceof KeymapEntry.PrefixBinding pb) {
                current = pb.keymap();
            } else {
                var newMap = new Keymap(prefix.displayString());
                current.bindPrefix(prefix, newMap);
                current = newMap;
            }
        }
        current.bind(keyStrokes.get(keyStrokes.size() - 1), command);
    }

    /**
     * メジャーモードファクトリを登録する。同名のモードが既に存在する場合は上書きする。
     * Python側からモードファクトリ（呼び出し可能なValue）が渡される。
     * ファクトリを一度呼び出してモード名を取得し、以降はファクトリとして登録する。
     *
     * @param modeFactory 呼び出すとMajorModeインスタンスを返すValue
     */
    public void registerMajorMode(Value modeFactory) {
        MajorMode probe = modeFactory.execute().as(MajorMode.class);
        String name = probe.name();
        Supplier<MajorMode> factory = () -> modeFactory.execute().as(MajorMode.class);
        modeRegistry.registerOrReplaceMajorMode(name, factory);
    }

    /**
     * マイナーモードファクトリを登録する。同名のモードが既に存在する場合は上書きする。
     * Python側からモードファクトリ（呼び出し可能なValue）が渡される。
     *
     * @param modeFactory 呼び出すとMinorModeインスタンスを返すValue
     */
    public void registerMinorMode(Value modeFactory) {
        MinorMode probe = modeFactory.execute().as(MinorMode.class);
        String name = probe.name();
        Supplier<MinorMode> factory = () -> modeFactory.execute().as(MinorMode.class);
        modeRegistry.registerOrReplaceMinorMode(name, factory);
    }

    /**
     * メジャーモード有効化時のフックを追加する。
     *
     * @param modeName フックを紐付けるモード名
     * @param hook 有効化時に実行される関数
     */
    public void addMajorModeHook(String modeName, Value hook) {
        modeRegistry.addMajorModeHook(modeName, (buffer, mode) -> hook.execute(buffer, mode));
    }

    /**
     * マイナーモード有効化時のフックを追加する。
     *
     * @param modeName フックを紐付けるモード名
     * @param hook 有効化時に実行される関数
     */
    public void addMinorModeHook(String modeName, Value hook) {
        modeRegistry.addMinorModeHook(modeName, (buffer, mode) -> hook.execute(buffer, mode));
    }

    /**
     * 拡張子とメジャーモード名のマッピングを登録する。
     * 指定されたモード名がModeRegistryに登録されていない場合は例外をスローする。
     *
     * @param extension 拡張子（ドットなし、例: "py"）
     * @param modeName モード名
     * @throws IllegalArgumentException 指定されたモード名が未登録の場合
     */
    public void registerAutoMode(String extension, String modeName) {
        Supplier<MajorMode> factory = modeRegistry
                .lookupMajorMode(modeName)
                .orElseThrow(() -> new IllegalArgumentException("メジャーモード '" + modeName + "' は登録されていません"));
        autoModeMap.register(extension, factory);
    }

    /**
     * 指定言語の言語サポート（スタイラーとアナライザー）を生成する。
     * 同一セッションを共有するスタイラーとアナライザーの組を返す。
     * 未対応の言語の場合はnullを返す。
     *
     * @param language 言語名（例: "python"）
     * @return 言語サポート、または未対応の場合null
     */
    public SyntaxAnalyzerRegistry.@Nullable LanguageSupport createLanguageSupport(String language) {
        return syntaxAnalyzerRegistry.create(language).orElse(null);
    }

    /**
     * 現在のフレーム状態を名前付きで保存する。
     *
     * @param name 保存名
     */
    public void saveFrameState(String name) {
        var snapshot = frame.captureSnapshot();
        frameLayoutStore.save(name, snapshot);
    }

    /**
     * 保存済みフレーム状態を名前で復元する。
     *
     * @param name 復元対象の保存名
     * @return 復元に成功した場合true、名前が見つからない場合false
     */
    public boolean restoreFrameState(String name) {
        var snapshot = frameLayoutStore.load(name);
        if (snapshot.isEmpty()) {
            return false;
        }
        var fallback = bufferManager
                .findByName(BufferNames.SCRATCH)
                .orElseThrow(() -> new IllegalStateException("scratch バッファが見つかりません"));
        frame.restoreSnapshot(snapshot.get(), bufferManager, fallback);
        return true;
    }

    /**
     * 指定名のフレーム状態が保存済みかどうかを返す。
     *
     * @param name 確認対象の保存名
     * @return 保存済みの場合true
     */
    public boolean hasFrameState(String name) {
        return frameLayoutStore.load(name).isPresent();
    }
}
