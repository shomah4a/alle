package io.github.shomah4a.alle.core.mode;

import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;

/**
 * ファイル拡張子からメジャーモードへのマッピング。
 * 拡張子に対応するモードファクトリを登録し、ファイル名からモードを解決する。
 */
public class AutoModeMap {

    private final MutableMap<String, Supplier<MajorMode>> extensionMap;
    private final Supplier<MajorMode> defaultMode;

    public AutoModeMap(Supplier<MajorMode> defaultMode) {
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
     * ファイル名からメジャーモードを解決する。
     * 拡張子にマッチするモードがあればそのインスタンスを、なければデフォルトモードを返す。
     */
    public MajorMode resolve(String fileName) {
        return findExtension(fileName)
                .flatMap(ext -> Optional.ofNullable(extensionMap.get(ext)))
                .map(Supplier::get)
                .orElseGet(defaultMode);
    }

    private Optional<String> findExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(fileName.substring(dotIndex + 1));
    }
}
