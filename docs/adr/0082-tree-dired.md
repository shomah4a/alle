# ADR 0082: ツリー表示 Dired

## ステータス

承認済み

## コンテキスト

ディレクトリの内容をブラウズする機能が必要である。
Emacs の dired に相当するが、フラットな一覧ではなくツリー表示とし、ディレクトリの展開/折り畳みによるドリルダウンを可能にする。
即全展開はパフォーマンス・可読性の両面で問題があるため、遅延展開とする。

## 決定

### パッケージ構成

`io.github.shomah4a.alle.core.mode.modes.dired` パッケージに配置する。

### データモデル

#### TreeDiredEntry (record)

ツリー上の1行分のエントリを表す。

```java
record TreeDiredEntry(Path path, int depth, boolean isDirectory, boolean isExpanded)
```

#### TreeDiredModel

ツリーの展開状態を管理するモデル。

- ルートディレクトリの Path を保持
- 展開済みディレクトリの `MutableSet<Path>` を保持
- `DirectoryLister` を使ってディレクトリ内容を取得（副作用の外部化）
- `toggle(Path)` で展開/折り畳みを切り替え
- `getVisibleEntries()` で現在表示すべきエントリのリストを返す
- 初期状態: ルートディレクトリのみ展開

### バッファとモデルの紐付け

TreeDiredMode を MajorMode として実装し、バッファごとに個別インスタンスを生成する。
TreeDiredModel を TreeDiredMode のフィールドとして保持する。
コマンドから `context.activeWindow().getBuffer().getMajorMode()` でアクセスする。

### read-only バッファの内容更新

バッファは read-only に設定し、ユーザーからの直接編集を防ぐ。
展開/折り畳み等でバッファ内容を更新する際は、`atomicOperation` 内で `setReadOnly(false)` → テキスト書き換え → `setReadOnly(true)` を行う。

### self-insert-command の抑制

TreeDiredMode のキーマップに no-op の `defaultCommand` を設定する。
キー解決順序でメジャーモードキーマップはグローバルキーマップより優先されるため、未バインドの印字可能文字キーは no-op となり "Text is read-only" メッセージが抑制される。

### 表示形式

```
/home/user/project
▼ src/
    ▶ main/
    ▶ test/
▶ docs/
  README.md
  build.gradle
```

- インデント: depth × 2 スペース
- ディレクトリ: 展開済みは "▼ "、未展開は "▶ "、末尾に "/"
- ファイル: "  " (2スペース) + ファイル名
- 1行目にルートディレクトリのフルパスをヘッダとして表示
- face によるスタイリング: ディレクトリ名とファイル名で異なるスタイル

### コマンド

| コマンド名 | キーバインド | 動作 |
|---|---|---|
| tree-dired | C-x d | ディレクトリを指定して Tree Dired バッファを開く |
| tree-dired-toggle | TAB | カーソル行のディレクトリを展開/折り畳み |
| tree-dired-find-file-or-toggle | Enter | ファイルなら開く、ディレクトリなら展開/折り畳み |
| tree-dired-up-directory | ^ | ルートディレクトリの親に移動 |
| tree-dired-refresh | g | 展開状態を保持したまま再読み込み |
| kill-buffer | q | バッファを閉じる (既存コマンドに委譲) |

### キーバインド

TreeDiredMode のメジャーモードキーマップに上記コマンドをバインドする。
カーソル移動等の基本操作はグローバルキーマップにフォールスルーする。

### find-file からのディレクトリ対応

find-file (C-x C-f) でディレクトリパスを指定した場合、Tree Dired バッファで開く。
ディレクトリ判定は `Predicate<Path>` として外部注入し、副作用の外部化を維持する。
FindFileCommand に `setTreeDiredCommand` で TreeDiredCommand を設定し、
ディレクトリの場合は `TreeDiredCommand.openDiredForPath` に委譲する。

## 影響範囲

- 新規パッケージ `io.github.shomah4a.alle.core.mode.modes.dired` の追加
- `EditorCore.java` にコマンド登録と C-x d キーバインドを追加
- `FaceName.java` / `DefaultFaceTheme.java` に dired 用の face を追加
- `DirectoryEntry` に `FileAttributes` フィールドを追加
- `FileAttributes` record を新規追加
- `FindFileCommand` にディレクトリ判定と TreeDiredCommand への委譲を追加
