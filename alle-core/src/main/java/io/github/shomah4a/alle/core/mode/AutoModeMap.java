package io.github.shomah4a.alle.core.mode;

import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;

/**
 * ファイル名・拡張子からメジャーモードへのマッピング。
 * ファイル名完全一致または拡張子に対応するモードファクトリを登録し、
 * ファイル名からモードを解決する。
 *
 * <p>解決の優先順位: ファイル名完全一致 &gt; 拡張子マッチ &gt; デフォルトモード
 */
public class AutoModeMap {

    private final MutableMap<String, Supplier<MajorMode>> fileNameMap;
    private final MutableMap<String, Supplier<MajorMode>> extensionMap;
    private final Supplier<MajorMode> defaultMode;

    public AutoModeMap(Supplier<MajorMode> defaultMode) {
        this.fileNameMap = Maps.mutable.empty();
        this.extensionMap = Maps.mutable.empty();
        this.defaultMode = defaultMode;
    }

    /**
     * 拡張子に対応するモードファクトリを登録する。
     * 拡張子はドットなしで指定する（例: "txt", "java"）。
     */
    public void register(String extension, Supplier<MajorMode> modeFactory) {
        extensionMap.put(extension, modeFactory);
    }

    /**
     * ファイル名完全一致に対応するモードファクトリを登録する。
     * パス区切り文字は含めず、ファイル名のみを指定する（例: "Makefile"）。
     */
    public void registerFileName(String fileName, Supplier<MajorMode> modeFactory) {
        fileNameMap.put(fileName, modeFactory);
    }

    /**
     * ファイル名からメジャーモードを解決する。
     * ファイル名完全一致、拡張子マッチ、デフォルトモードの順に検索する。
     */
    public MajorMode resolve(String fileName) {
        String baseName = extractBaseName(fileName);

        // ファイル名完全一致
        var fileNameFactory = fileNameMap.get(baseName);
        if (fileNameFactory != null) {
            return fileNameFactory.get();
        }

        // 拡張子マッチ
        return findExtension(baseName)
                .flatMap(ext -> Optional.ofNullable(extensionMap.get(ext)))
                .map(Supplier::get)
                .orElseGet(defaultMode);
    }

    private static String extractBaseName(String fileName) {
        int lastSep = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        return lastSep >= 0 ? fileName.substring(lastSep + 1) : fileName;
    }

    private Optional<String> findExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(fileName.substring(dotIndex + 1));
    }
}
