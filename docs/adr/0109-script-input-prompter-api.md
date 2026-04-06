# ADR 0109: スクリプトからInputPrompter経由で入力を受け取るAPI

## ステータス

承認済み

## コンテキスト

Pythonスクリプトからユーザーにミニバッファ経由で入力を求める手段がない。
Java側では `InputPrompter` が `CompletableFuture<PromptResult>` を返す非同期APIとして存在し、
`FindFileCommand` 等のJavaコマンドで利用されているが、スクリプト層には公開されていない。

## 決定

### Java側

- `EditorCore` に `inputPrompter` のgetterを追加する
- `EditorFacade` に `InputPrompter` を注入し、以下のAPIを公開する:
  - `prompt(String message, InputHistory history)` → `CompletableFuture<PromptResult>`
  - `createInputHistory()` → `InputHistory`

### Python側

- `alle/prompt.py` を新設し、以下のクラスを提供する:
  - `Confirmed(value)` / `Cancelled()` — PromptResultのPythonラッパー
  - `PromptFuture` — `CompletableFuture<PromptResult>` のラッパー
    - `.then(fn)` — 非同期チェイン
    - `.on_confirmed(fn)` / `.on_cancelled(fn)` — 型別コールバック
    - `.result()` — ブロッキング取得
    - `.is_done()` — 完了チェック
- `alle/history.py` を新設し、`InputHistory` ラッパーを提供する
- `alle.__init__.py` にモジュールレベルAPI を追加する:
  - `prompt(message, history=None, initial_value=None)` → `PromptFuture`
  - `create_input_history()` → `InputHistory`

### 非同期設計

`InputPrompter.prompt()` はミニバッファの入力確定/キャンセルで完了する `CompletableFuture` を返す。
ミニバッファの入力処理はコマンドループスレッドで行われるため、
Python側では `.then()` による非同期チェインで利用する。

## 使用例

```python
import alle
from alle.history import InputHistory

history = InputHistory()

@alle.command("my-input-command")
def my_input(ctx):
    alle.prompt("Enter name: ", history=history).on_confirmed(
        lambda value: ctx.message(f"Hello, {value}")
    )
```

## 将来の拡張

- Completer（補完機能）のスクリプト対応
- ScriptCommand.execute() の返り値をプロンプト完了に連動させる改修（undoトランザクション統合）
