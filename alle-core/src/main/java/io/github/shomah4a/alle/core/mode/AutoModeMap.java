package io.github.shomah4a.alle.core.mode;

import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;

/**
 * ファイル名・拡張子・shebangからメジャーモードへのマッピング。
 * 以下のいずれかのキーにモードファクトリを登録し、ファイル名および先頭行からモードを解決する。
 *
 * <ul>
 *   <li>ファイル名完全一致</li>
 *   <li>拡張子</li>
 *   <li>shebang インタプリタのbasename（例: "bash", "python3"）</li>
 * </ul>
 *
 * <p>解決の優先順位: ファイル名完全一致 &gt; 拡張子マッチ &gt; shebangマッチ &gt; デフォルトモード
 *
 * <p>この優先順位は Emacs の {@code interpreter-mode-alist} が {@code auto-mode-alist} より
 * 低い優先で動作する挙動に準拠している。
 */
public class AutoModeMap {

    private final MutableMap<String, Supplier<MajorMode>> fileNameMap;
    private final MutableMap<String, Supplier<MajorMode>> extensionMap;
    private final MutableMap<String, Supplier<MajorMode>> shebangMap;
    private final Supplier<MajorMode> defaultMode;

    public AutoModeMap(Supplier<MajorMode> defaultMode) {
        this.fileNameMap = Maps.mutable.empty();
        this.extensionMap = Maps.mutable.empty();
        this.shebangMap = Maps.mutable.empty();
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
     * shebangインタプリタのbasenameに対応するモードファクトリを登録する。
     * 例: "bash", "python3"。
     */
    public void registerShebang(String command, Supplier<MajorMode> modeFactory) {
        shebangMap.put(command, modeFactory);
    }

    /**
     * ファイル名からメジャーモードを解決する。
     * shebangは参照せず、ファイル名完全一致 &gt; 拡張子マッチ &gt; デフォルトモードの順に解決する。
     */
    public MajorMode resolve(String fileName) {
        String baseName = extractBaseName(fileName);
        return resolveByName(baseName).orElseGet(defaultMode);
    }

    /**
     * ファイル名および先頭行（shebang）からメジャーモードを解決する。
     * 解決順位: ファイル名完全一致 &gt; 拡張子マッチ &gt; shebangマッチ &gt; デフォルトモード。
     *
     * <p>firstLineSupplier はファイル名と拡張子でマッチしなかった場合にのみ呼ばれる。
     * 空文字列を返す実装は shebang マッチをスキップすることを意味する。
     */
    public MajorMode resolve(String fileName, Supplier<String> firstLineSupplier) {
        String baseName = extractBaseName(fileName);
        var byName = resolveByName(baseName);
        if (byName.isPresent()) {
            return byName.get();
        }
        return parseShebangCommand(firstLineSupplier.get())
                .flatMap(command -> Optional.ofNullable(shebangMap.get(command)))
                .map(Supplier::get)
                .orElseGet(defaultMode);
    }

    private Optional<MajorMode> resolveByName(String baseName) {
        // ファイル名完全一致
        var fileNameFactory = fileNameMap.get(baseName);
        if (fileNameFactory != null) {
            return Optional.of(fileNameFactory.get());
        }

        // 拡張子マッチ
        return findExtension(baseName)
                .flatMap(ext -> Optional.ofNullable(extensionMap.get(ext)))
                .map(Supplier::get);
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

    /**
     * shebang行からインタプリタコマンドのbasenameを抽出する。
     * 先頭行が "#!" で始まらない場合、または抽出できない場合は空を返す。
     *
     * <p>パース規則:
     * <ul>
     *   <li>"#!" 以降を空白（スペース・タブ）で分割する</li>
     *   <li>最初のトークンをインタプリタパスとし、そのbasenameを取得する</li>
     *   <li>basenameが "env" の場合、続くトークンを順に調べ、
     *       "-" で始まるもの（envオプション）と "=" を含むもの（変数設定）はスキップし、
     *       最初に該当しないトークンのbasenameを採用する</li>
     * </ul>
     */
    static Optional<String> parseShebangCommand(String firstLine) {
        if (firstLine == null || firstLine.length() < 2) {
            return Optional.empty();
        }
        if (firstLine.charAt(0) != '#' || firstLine.charAt(1) != '!') {
            return Optional.empty();
        }
        String body = firstLine.substring(2).trim();
        if (body.isEmpty()) {
            return Optional.empty();
        }
        MutableList<String> tokens = tokenizeBySpaceOrTab(body);
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        String first = basenameOfPath(tokens.get(0));
        if (!"env".equals(first)) {
            return first.isEmpty() ? Optional.empty() : Optional.of(first);
        }
        for (int i = 1; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.isEmpty() || token.charAt(0) == '-' || token.indexOf('=') >= 0) {
                continue;
            }
            String name = basenameOfPath(token);
            return name.isEmpty() ? Optional.empty() : Optional.of(name);
        }
        return Optional.empty();
    }

    private static MutableList<String> tokenizeBySpaceOrTab(String body) {
        MutableList<String> tokens = Lists.mutable.empty();
        int length = body.length();
        int index = 0;
        while (index < length) {
            while (index < length && isSpaceOrTab(body.charAt(index))) {
                index++;
            }
            if (index >= length) {
                break;
            }
            int start = index;
            while (index < length && !isSpaceOrTab(body.charAt(index))) {
                index++;
            }
            tokens.add(body.substring(start, index));
        }
        return tokens;
    }

    private static boolean isSpaceOrTab(char c) {
        return c == ' ' || c == '\t';
    }

    private static String basenameOfPath(String path) {
        int lastSep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSep >= 0 ? path.substring(lastSep + 1) : path;
    }
}
