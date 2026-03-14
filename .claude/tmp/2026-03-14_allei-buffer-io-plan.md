# Allei - バッファモデル・IO実装計画

## 実装スコープ

バッファのモデルとIO周りを実装し、テストを書く。

## モジュール・パッケージ構成

### libs/gap-buffer

- パッケージ: `io.github.shomah4a.allei.libs.gapbuffer`
- `GapModel` — 純粋なGap Bufferデータ構造
- エディタに依存しない汎用ライブラリ

### allei-core

- パッケージ: `io.github.shomah4a.allei.core.textmodel`
  - `TextModel` (interface) — テキストモデルの契約
  - `GapTextModel` — GapModelをラップしてTextModelを実装
- パッケージ: `io.github.shomah4a.allei.core.buffer`
  - `Buffer` — TextModelを保持、ファイルパス、変更フラグ、カーソル位置等
  - `BufferManager` — 複数バッファの管理
- パッケージ: `io.github.shomah4a.allei.core.io`
  - ファイル読み込み → Buffer生成
  - Buffer → ファイル保存
  - 改行コードは内部LF正規化、IO層で変換
  - 副作用はインターフェース経由で注入可能にする

### 依存方向

`allei-core` → `libs/gap-buffer`（一方向）

## 実装ステップ

### 1. プロジェクト骨格の作成

- Gradle マルチプロジェクト構成セットアップ (Kotlin DSL)
- settings.gradle.kts に libs/gap-buffer, allei-core を登録
- Java 21, JUnit 5
- 開発ブランチ作成

### 2. libs/gap-buffer - GapModel

- char配列ベースのGap Buffer実装
- 基本操作: insert, delete, charAt, substring, length
- ユニットテスト
  - 空バッファへの挿入/削除
  - 先頭/末尾での操作
  - ギャップサイズ0直後の操作
  - 範囲外アクセス時の例外
  - 大量テキスト

### 3. allei-core - TextModel インターフェース

- Bufferが必要とする操作から逆算してメソッドセットを確定
- insert, delete, charAt, substring, length
- lineCount, lineStartOffset, lineText (改行コードはLF前提)

### 4. allei-core - GapTextModel

- GapModelをメンバに持ち、TextModelを実装
- 行関連のロジックはこのクラスの責務
- ユニットテスト

### 5. allei-core - Buffer, BufferManager

- Buffer: TextModel保持、ファイルパス、変更フラグ(dirty)、カーソル位置(point)
- テキスト変更時のカーソル位置自動調整
- BufferManager: バッファの追加/削除/切り替え
- ユニットテスト

### 6. allei-core - IO

- ファイル読み書きのインターフェース定義
- 実装: UTF-8、改行コード正規化(読み込み時LFに変換、保存時は元の改行コードを保持)
- ユニットテスト(インメモリ実装を利用)

### 7. テスト全体実行・確認

## 設計方針

- 改行コード: TextModel内部はLF。IO層で読み書き時に変換
- 副作用の外部化: ファイルI/Oはインターフェース経由で注入
- スレッドセーフティ: 現時点では単一スレッド前提
- Gap Bufferの不変条件はassertで記述
