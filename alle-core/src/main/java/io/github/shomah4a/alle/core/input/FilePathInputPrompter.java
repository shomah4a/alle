package io.github.shomah4a.alle.core.input;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.styling.FaceName;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.list.ListIterable;

/**
 * ファイルパス入力に特化したプロンプタ。
 * 汎用の{@link InputPrompter}を内部に保持し、以下のファイルパス固有の機能を提供する。
 *
 * <ul>
 *   <li>HOMEディレクトリの ~ 表示と展開</li>
 *   <li>シャドウパス表示（/~ や // による新ルート指定時に先行パスを薄く表示）</li>
 *   <li>確定時のシャドウ除去と ~ 展開</li>
 *   <li>シャドウ対応の補完（有効パスのみで補完し、候補にシャドウプレフィックスを再付与）</li>
 * </ul>
 */
public class FilePathInputPrompter {

    private final InputPrompter inputPrompter;
    private final DirectoryLister directoryLister;
    private final Path homeDirectory;

    public FilePathInputPrompter(InputPrompter inputPrompter, DirectoryLister directoryLister, Path homeDirectory) {
        this.inputPrompter = inputPrompter;
        this.directoryLister = directoryLister;
        this.homeDirectory = homeDirectory;
    }

    /**
     * ファイルパス入力プロンプトを表示する。
     * 初期値のHOME部分は ~ に置換され、確定結果は ~ 展開・シャドウ除去済みの絶対パス相当で返る。
     *
     * @param message      プロンプトメッセージ
     * @param initialPath  初期パス（絶対パス）
     * @param history      入力履歴（~ 形式・シャドウ除去済みの値が追加される）
     * @return 入力結果（確定時の value は ~ 展開・シャドウ除去済み）
     */
    public CompletableFuture<PromptResult> prompt(String message, String initialPath, InputHistory history) {
        String displayInitialValue = PathResolver.collapseTilde(initialPath, homeDirectory) + "/";
        var filePathCompleter = new FilePathCompleter(directoryLister, homeDirectory);
        var shadowCompleter = new ShadowAwareCompleter(filePathCompleter);

        return inputPrompter
                .prompt(message, displayInitialValue, history, shadowCompleter, this::onUpdate)
                .thenApply(result -> {
                    if (result instanceof PromptResult.Confirmed confirmed) {
                        String raw = confirmed.value();
                        // シャドウ除去
                        int boundary = PathResolver.findShadowBoundary(raw);
                        String effective = boundary > 0 ? raw.substring(boundary) : raw;
                        // ~ 展開
                        String expanded = PathResolver.expandTilde(effective, homeDirectory);
                        history.add(effective);
                        return new PromptResult.Confirmed(expanded);
                    }
                    return result;
                });
    }

    /**
     * テキスト変更時のコールバック。シャドウ face を更新する。
     */
    private void onUpdate(BufferFacade minibuffer, int promptLength, String currentInput) {
        int boundary = PathResolver.findShadowBoundary(currentInput);

        // 既存のシャドウ face を除去
        int fullLength = minibuffer.length();
        if (fullLength > promptLength) {
            minibuffer.removeFaceByName(promptLength, fullLength, FaceName.FILE_NAME_SHADOW);
        }

        // シャドウ境界がある場合のみ face を適用
        if (boundary > 0) {
            minibuffer.putFace(promptLength, promptLength + boundary, FaceName.FILE_NAME_SHADOW);
        }
    }

    /**
     * シャドウ対応の補完ラッパー。
     * ミニバッファの入力全体からシャドウ部分を除去して有効パスのみで補完し、
     * 候補にシャドウプレフィックスを再付与して返す。
     */
    private static class ShadowAwareCompleter implements Completer {

        private final FilePathCompleter delegate;

        ShadowAwareCompleter(FilePathCompleter delegate) {
            this.delegate = delegate;
        }

        @Override
        public ListIterable<CompletionCandidate> complete(String input) {
            int boundary = PathResolver.findShadowBoundary(input);
            String shadowPrefix = input.substring(0, boundary);
            String effectiveInput = input.substring(boundary);

            if (effectiveInput.isEmpty()) {
                return delegate.complete(effectiveInput);
            }

            var candidates = delegate.complete(effectiveInput);

            if (shadowPrefix.isEmpty()) {
                return candidates;
            }

            // 候補にシャドウプレフィックスを再付与
            return candidates.collect(c -> {
                String valueWithShadow = shadowPrefix + c.value();
                return c.terminal()
                        ? CompletionCandidate.terminal(valueWithShadow, c.label())
                        : CompletionCandidate.partial(valueWithShadow, c.label());
            });
        }
    }
}
