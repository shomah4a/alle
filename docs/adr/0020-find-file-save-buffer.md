# ADR-0020: find-file / save-buffer コマンド

## ステータス

承認

## コンテキスト

エディタとしてファイルの読み込みと保存は基本機能である。
BufferIO基盤（ADR-0005）とInputPrompter基盤（ADR-0019）が整ったため、
find-file（C-x C-f）とsave-buffer（C-x C-s）を実装する。

## 決定

### find-file

- InputPrompterで「Find file: 」とプロンプトし、ファイルパスを入力させる
- BufferIO.loadでファイルを読み込み、BufferManagerに追加してウィンドウを切り替える
- 同一パスのバッファが既に開かれている場合はそのバッファに切り替える
- ファイルが存在しない場合は空バッファをファイルパス付きで作成する（Emacsの挙動）
- BufferIOはコンストラクタで注入する（副作用の外部化）

### save-buffer

- バッファにファイルパスがある場合はそのまま保存する
- ファイルパスがない場合はInputPrompterでパスを入力させる
- LineEndingはBufferが保持する（デフォルトLF、load時に検出値を設定）
- IO例外はログ出力する（ユーザー向けメッセージ表示は将来課題）

### LineEnding管理

- BufferにLineEndingフィールドを追加する（デフォルトLF）
- BufferIO.saveはBuffer.getLineEnding()を使用し、引数からLineEndingを除去する
- BufferIO.loadではバッファにLineEndingを設定する

### 同名バッファ問題

- BufferManagerにfindByPath(Path)を追加し、パスベースの重複チェックを行う
- find-fileでは新規作成前にfindByPathで既存バッファを検索する

## 影響

- Buffer: LineEndingフィールド追加
- BufferIO: saveシグネチャ変更、loadでLineEndingをバッファに設定
- BufferManager: findByPath追加
- Main.java: コマンド登録、C-x プレフィックスキーマップ追加
