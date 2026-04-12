package io.github.shomah4a.alle.core.statusline;

/**
 * gitブランチ・ステータスを表示するステータスラインスロット。
 * バッファがファイルに関連付けられていない場合、またはgit管理外の場合は空文字列を返す。
 */
public final class GitStatusSlot implements StatusLineElement {

    private final GitBranchProvider branchProvider;

    public GitStatusSlot(GitBranchProvider branchProvider) {
        this.branchProvider = branchProvider;
    }

    @Override
    public String name() {
        return "git-status";
    }

    @Override
    public String render(StatusLineContext context) {
        var filePathOpt = context.buffer().getFilePath();
        if (filePathOpt.isEmpty()) {
            return "";
        }
        var branchInfoOpt = branchProvider.getBranch(filePathOpt.get());
        if (branchInfoOpt.isEmpty()) {
            return "";
        }
        var info = branchInfoOpt.get();
        String dirtyMark = info.dirty() ? "*" : "";
        return " Git:" + info.branchName() + dirtyMark;
    }
}
