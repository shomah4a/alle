# ADR 0108: スクリプトからのフレーム状態操作API

## ステータス

承認

## コンテキスト

Python スクリプトから `save-frame-state` / `restore-frame-state` 相当の操作をプログラム的に行いたい。
現状、`FrameLayoutStore` へのアクセスは Java 側のコマンド実装にのみ閉じており、スクリプト層には公開されていない。

また、`"*scratch*"` バッファ名が複数箇所で文字列リテラルとしてハードコードされており、定数化されていない。

## 決定

### 定数の整理

- `alle-core` に `constants` パッケージを新設し、`BufferNames` クラスに `SCRATCH = "*scratch*"` を定義する
- 既存のハードコード箇所（`EditorCore`, `KillBufferCommand`）をこの定数に置き換える

### スクリプトAPI

- `EditorFacade` に `FrameLayoutStore` と `BufferManager` への参照を追加する
- 以下のメソッドを公開する:
  - `saveFrameState(String name)` — 現在のフレーム状態を保存
  - `restoreFrameState(String name)` — 名前指定で復元（成否を boolean で返す）
  - `hasFrameState(String name)` — 存在確認
- Python 側の `alle` モジュールに対応するラッパー関数を追加する:
  - `save_frame_state(name)`
  - `restore_frame_state(name)` → `bool`
  - `has_frame_state(name)` → `bool`

### fallback バッファの取得

- `restoreFrameState` 内の fallback バッファは `BufferManager.findByName(BufferNames.SCRATCH)` で取得する

## 影響

- `EditorFacade` のコンストラクタに引数が増える（`FrameLayoutStore`, `BufferManager`）
- `Main.java` および既存テストの `EditorFacade` 生成箇所を修正する必要がある
