# ADR 0140: git-mode と git-log コマンド

## ステータス

Accepted

## コンテキスト

alle にはこれまで `tree-dired-git` マイナーモード (tree-dired 特化。git status カラム / branch suffix / add/delete/rename の tree-dired ファイル操作 hook 版) のみが存在し、ファイルバッファやリポジトリ横断の git 操作は提供されていなかった。

Emacs の `vc-print-log` 相当の履歴閲覧を、任意のメジャーモードのバッファから呼び出せるようにする必要が生じた。

## 決定

### モード構成

- 新設マイナーモード `git-mode` を追加する
- git リポジトリ配下のファイルまたはディレクトリを開いたときに自動有効化する
- 既存 `tree-dired-git` は温存する。責務境界を分けて独立に動作する:

| 機能 | tree-dired-git | git-mode |
|---|---|---|
| status カラム | ○ | × |
| branch suffix | ○ | × |
| add/delete/rename | ○ (tree-dired 前提) | × |
| git-log | × | ○ |
| 対象バッファ | tree-dired のみ | ファイル・tree-dired 両方 |

tree-dired バッファでは両モードが独立に enable される (`tree-dired-git` / `git` の名前で区別)。`git-mode` はバッファ変数を書き込まないため、`tree-dired-git` の変数と競合しない。

### 自動有効化 hook

- `ModeRegistry` に `addAllMajorModeHook(BiConsumer<BufferFacade, String>)` を追加する
- `runMajorModeHooks(modeName, buffer)` の末尾で共通 hook を全件実行する
- **順序契約**: name-scoped hook が先、共通 hook が後。共通 hook (git-mode の enable) が tree-dired バッファに対して発火するときは `tree-dired-git` の enable の後になる。順序は `ModeRegistryAllHookTest` の assertion で担保する
- hook 本体は `GitModeInitializer` 内で `buffer.getFilePath().isEmpty()` を早期 return、tree-dired バッファなら `((TreeDiredMode) majorMode).getModel().getRootDirectory()` を、それ以外は filePath を起点に `GitRepositoryLocator` でリポジトリルートを探索し、リポジトリ内なら `git-mode` を enable する

### リポジトリルート探索

- `mode/modes/git/GitRepositoryLocator` を新設し、Path (file または dir) から親を遡って `.git` (dir または file / worktree 対応) を持つディレクトリを `Optional<Path>` で返す
- hook から高頻度で呼ばれるため Caffeine キャッシュ (5 秒 TTL / 100 件) を持たせる
- 既存 `CachingGitBranchProvider.resolveGitRoot` と機能重複するため、`CachingGitBranchProvider` 側を `GitRepositoryLocator` に委譲する形にリファクタリングしキャッシュを一本化する

### GitMode のライフサイクル

- `onEnable` / `onDisable` はともに no-op とする
- バッファ変数を書き込まないため、M-x `git-mode` によるトグルでも副作用なし
- `settingDefaults()` で以下の設定変数を登録する:
  - `git-log-subject-max-width` (デフォルト値は実装時に決定)
  - `git-log-body-max-lines` (デフォルト値は実装時に決定)

### git-log コマンド

- 対象 Path: バッファに紐づく Path (ファイルバッファ→そのファイル、tree-dired→そのディレクトリ、なければリポジトリルート全体)
- 表示形式: **short-header** (Emacs vc-log 準拠。`commit <hash>` / `Author:` / `Date:` / 空行 / インデント付き subject / 区切り)
- 長いコミットメッセージの truncate 幅・行数は `git-log-subject-max-width` / `git-log-body-max-lines` の設定変数で制御する

### スレッドモデル (非同期実行)

- `GitLogCommand.execute` は `CompletableFuture` を返し、subprocess は共有 executor 経由で UI スレッド外に飛ばす
- 完了後にコマンドループへ戻ってバッファ生成/上書き・split を行う
- `DefaultGitBranchProvider` の同期実装 (statusline から呼ばれ UI スレッドで最大 3 秒フリーズしうる) と同じ轍を踏まないための決定

### バッファ名の衝突回避

- 結果バッファ名: `*git-log: <relative-path> [dir|file|repo]*`
- `[dir]` / `[file]` / `[repo]` サフィックスで対象種別を分離し、同一 relative-path で異なる対象種別のバッファが衝突しないようにする

### read-only 一時解除

- `GitLogMode` は read-only。render 時は `atomicOperation { setReadOnly(false); render(); markClean(); setReadOnly(true); }` パターンを踏襲する

### コマンド登録とキーマップ

- `GitLogCommand` はグローバル `CommandRegistry` に登録する (M-x `git-log` で呼び出し可能)
- `GitMode` のマイナーモード keymap: `C-x v l` = `git-log`
- `GitLogMode` の keymap: `q` = kill-buffer

## 結果

- 任意のメジャーモードのバッファから `git-log` で履歴閲覧が可能になる
- 既存の `tree-dired-git` の挙動は変わらない
- ファイル open のたびに親遡り FS stat が走るが、`GitRepositoryLocator` のキャッシュで吸収される
- statusline の git branch 取得と git-log のリポジトリルート判定でキャッシュが統一される
- subprocess UI スレッドブロッキングリスクは非同期実行で回避される
