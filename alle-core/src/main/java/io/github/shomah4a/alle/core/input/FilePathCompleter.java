package io.github.shomah4a.alle.core.input;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;

/**
 * ファイルパスの補完を提供する。
 * 入力パスの親ディレクトリ内のエントリから前方一致で候補を返す。
 * ディレクトリ候補は継続補完（partial）、ファイル候補は確定可能（terminal）として返す。
 */
public class FilePathCompleter implements Completer {

    private static final Logger logger = Logger.getLogger(FilePathCompleter.class.getName());

    private final DirectoryLister directoryLister;
    private final Path homeDirectory;

    public FilePathCompleter(DirectoryLister directoryLister, Path homeDirectory) {
        this.directoryLister = directoryLister;
        this.homeDirectory = homeDirectory;
    }

    @Override
    public ListIterable<CompletionCandidate> complete(String input) {
        if (input.isEmpty()) {
            return Lists.immutable.empty();
        }

        // シャドウ部分を除去して有効パスのみで補完する
        int boundary = shadowBoundary(input);
        String shadowPrefix = input.substring(0, boundary);
        String effectiveInput = input.substring(boundary);

        if (effectiveInput.isEmpty()) {
            return Lists.immutable.empty();
        }

        // ~ を展開して実パスに変換
        String expanded = PathResolver.expandTilde(effectiveInput, homeDirectory);
        boolean useTilde = !expanded.equals(effectiveInput);

        // 末尾が "/" の場合はそのディレクトリの中身を一覧する
        if (expanded.endsWith("/")) {
            Path directory = Path.of(expanded);
            try {
                return toCompletionCandidates(directoryLister.list(directory), useTilde, shadowPrefix);
            } catch (IOException e) {
                logger.log(Level.FINE, "ディレクトリ一覧の取得に失敗: " + directory, e);
                return Lists.immutable.empty();
            }
        }

        Path inputPath = Path.of(expanded);
        Path parent = inputPath.getParent();
        if (parent == null) {
            parent = Path.of(".");
        }

        ListIterable<DirectoryEntry> entries;
        try {
            entries = directoryLister.list(parent);
        } catch (IOException e) {
            logger.log(Level.FINE, "ディレクトリ一覧の取得に失敗: " + parent, e);
            return Lists.immutable.empty();
        }

        String inputStr = inputPath.toString();
        return toCompletionCandidates(
                entries.select(entry -> entry.path().toString().startsWith(inputStr)), useTilde, shadowPrefix);
    }

    @Override
    public int shadowBoundary(String input) {
        return PathResolver.findShadowBoundary(input);
    }

    private ListIterable<CompletionCandidate> toCompletionCandidates(
            ListIterable<DirectoryEntry> entries, boolean useTilde, String shadowPrefix) {
        return entries.collect(entry -> {
            String pathStr = entry.path().toString();
            String displayPath = useTilde ? PathResolver.collapseTilde(pathStr, homeDirectory) : pathStr;
            String fileName = entry.path().getFileName().toString();
            String valueWithShadow = shadowPrefix + displayPath;
            return switch (entry) {
                case DirectoryEntry.Directory d -> CompletionCandidate.partial(valueWithShadow + "/", fileName + "/");
                case DirectoryEntry.File f -> CompletionCandidate.terminal(valueWithShadow, fileName);
            };
        });
    }
}
