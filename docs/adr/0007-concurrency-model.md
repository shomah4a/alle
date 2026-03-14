# ADR-0007: 並行処理モデル

## ステータス

承認済み（未実装）

## コンテキスト

Emacsはシングルスレッド設計であり、重い処理（大ファイルの検索、外部プロセス連携等）でUIがブロックされる。
Alleiではこの問題を解消したい。

## 検討した選択肢

### A. 低レイヤー（GapModel/TextModel）をスレッドセーフにする

- synchronized や ReadWriteLock で個々の操作を保護
- 単純だが、複合操作のアトミック性は保証できない
- ロック競合でパフォーマンスが落ちやすい

### B. Buffer レベルでスレッドセーフにする

- Bufferに対する操作をまとめてロックする
- 複合操作のアトミック性を担保しやすい
- 長時間ロックで他スレッドがブロックされるリスク

### C. バッファ単位のアクターモデル

- バッファごとに専用スレッド＋メッセージキューを持つ
- テキストモデル自体はシングルスレッドアクセスなので同期不要
- バッファ間で独立しており、あるバッファの重い処理が他をブロックしない

### D. Immutableデータ構造 + MVCC的アプローチ

- 変更のたびにスナップショットを生成
- 読み取りはロック不要
- Gap Bufferとは相性が悪い（Ropeなら自然）

## 決定

**C. バッファ単位のアクターモデル**を採用する。

具体的な設計:

- 既存の `Buffer` / `TextModel` には手を入れない（シングルスレッド前提のまま）
- その上に `AsyncBuffer` 等のラッパーを被せ、スレッドセーフな操作を提供する
- 各バッファは専用のシングルスレッドExecutor（Virtual Thread）を持つ
- 外部からの操作はすべてメッセージキュー経由で専用スレッドに投入される
- 操作結果は `CompletableFuture` で返し、コマンド層でPromise的に扱える
- I/O、シンタックスハイライト、補完等の重い処理は別スレッドで非同期実行し、結果をキューに返す

```
UIスレッド
  ↓ コマンド送信 (CompletableFuture)
AsyncBuffer "foo.java" ← SingleThreadExecutor(Virtual Thread)
  └→ Buffer → TextModel → GapModel (すべて単一スレッド内)
AsyncBuffer "bar.txt"  ← SingleThreadExecutor(Virtual Thread)
  └→ Buffer → TextModel → GapModel
  ↓ 変更通知
UIスレッド(再描画)
```

## 帰結

- テキストモデル層にロックが不要でシンプルなまま
- バッファ間の独立性によりブロッキングを回避
- Java 21のVirtual Threadsにより、バッファごとにスレッドを立てるコストが低い
- バッファ横断操作（grep-replace等）は別途設計が必要

## 備考

コマンドシステムの実装時に合わせて導入する予定。
