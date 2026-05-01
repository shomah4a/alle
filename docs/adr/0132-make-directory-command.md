# ADR 0132: make-directory コマンドの追加

## ステータス

承認

## コンテキスト

ユーザーがエディタ内からディレクトリを作成できるコマンドが存在しない。
Emacs では `make-directory` / `mkdir` コマンドでディレクトリを作成でき、
dired モードでは `+` キーで `dired-create-directory` を呼び出せる。

## 決定

### グローバルコマンド

- コマンド名: `make-directory`（`mkdir` エイリアスあり）
- `FilePathInputPrompter` でパスを入力し、`FileOperations.createDirectories()` で作成する
- `mkdir -p` 相当の再帰的作成を行う
- キーバインドは割り当てない（`M-x` から呼び出す）

### tree-dired 用コマンド

- コマンド名: `tree-dired-make-directory`
- `+` キーにバインド
- カーソル行のディレクトリを初期パスとして入力を受ける
- 作成後に `TreeDiredBufferUpdater.update()` でバッファを更新する

### 副作用の外部化

- `FileOperations` インターフェースに `createDirectories(Path)` メソッドを追加
- `DefaultFileOperations` では `Files.createDirectories()` で実装
- テスト時は `StubFileOperations` で操作を記録

## 結果

- エディタ内からディレクトリを作成できるようになる
- tree-dired 内では作成後にバッファが自動更新され、新しいディレクトリが即座に表示される
- 副作用が外部化されており、テスタビリティが確保される
