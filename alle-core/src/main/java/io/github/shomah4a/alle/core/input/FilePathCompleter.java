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

    public FilePathCompleter(DirectoryLister directoryLister) {
        this.directoryLister = directoryLister;
    }

    @Override
    public ListIterable<CompletionCandidate> complete(String input) {
        if (input.isEmpty()) {
            return Lists.immutable.empty();
        }

        // 末尾が "/" の場合はそのディレクトリの中身を一覧する
        if (input.endsWith("/")) {
            Path directory = Path.of(input);
            try {
                return toCompletionCandidates(directoryLister.list(directory));
            } catch (IOException e) {
                logger.log(Level.FINE, "ディレクトリ一覧の取得に失敗: " + directory, e);
                return Lists.immutable.empty();
            }
        }

        Path inputPath = Path.of(input);
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
                entries.select(entry -> entry.path().toString().startsWith(inputStr)));
    }

    private static ListIterable<CompletionCandidate> toCompletionCandidates(ListIterable<DirectoryEntry> entries) {
        return entries.collect(entry -> {
            String pathStr = entry.path().toString();
            String fileName = entry.path().getFileName().toString();
            return switch (entry) {
                case DirectoryEntry.Directory d -> CompletionCandidate.partial(pathStr + "/", fileName + "/");
                case DirectoryEntry.File f -> CompletionCandidate.terminal(pathStr, fileName);
            };
        });
    }
}
