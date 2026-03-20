# ADR-0047: alleパッケージのリファクタリング

## ステータス

承認済み

## コンテキスト

GraalPyによる拡張用Pythonパッケージ `alle` は、公開APIと内部実装が同一の名前空間に混在している。
また、型アノテーションが一切付与されていないため、ユーザーがAPIを利用する際に引数や返り値の型が不明瞭である。

## 決定

### 1. 型アノテーションの追加

全Pythonファイルに `from __future__ import annotations` を追加し、関数・メソッドの引数と返り値に型アノテーションを付与する。
Java型（`java.type()` の戻り値）は `Any` として扱う。

### 2. `alle.internal` パッケージの導入

ユーザーから見えなくてよい内部実装を `alle.internal` パッケージに移動する。

#### 公開API（`alle` 直下に残すもの）

- `alle`: `active_window()`, `current_buffer()`, `message()`, `register_command()`, `global_set_key()`
- `alle.command`: `CommandBase`
- `alle.futures`: `JavaFuture`
- `alle.keybind`: `ctrl()`, `meta()`, `key()`
- `alle.mode`: `AlleMajorMode`, `AlleMinorMode`
- `alle.buffer`: `Buffer`
- `alle.window`: `Window`

#### 内部実装（`alle.internal` に移動するもの）

- `alle.internal.command`: `make_command()`, `_Cmd`
- `alle.internal.facade`: `_editor_facade`, `_init()`, `_require_facade()`, `_wrap_command()`
- `alle.internal.futures`: `wrap()`, `wrap_transform()`, `TransformedFuture`

### 制約

- Java側（`GraalPyEngine.java`）から `alle._init(_editor_facade)` が呼ばれるため、`alle.__init__` に `_init` のre-exportを残す。
- 既存の公開importパス（`from alle.command import CommandBase` 等）は維持する。

## 結果

- ユーザーが触るべきAPIと内部実装の境界が明確になる
- 型アノテーションによりAPIの使い方が自明になる
