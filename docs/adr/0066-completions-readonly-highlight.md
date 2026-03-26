# ADR 0066: Completionsバッファのread-only化と選択行ハイライト表示

## ステータス

提案

## コンテキスト

現在、*Completions* バッファは通常のEditableBufferで作成されており、ユーザーがother-windowで移動した際に文字入力で内容を変更できてしまう。
また、選択中の候補は `>` テキストマーカーで表示しているが、これは編集可能なテキストとして挿入されているためカーソル表示と紛らわしい。

### 関連する変更

1. EditableBuffer を TextBuffer にリネーム（汎用バッファ実装としてより適切な名前）
2. TextBuffer に read-only フラグを追加
3. Completionsバッファを read-only に設定
4. 選択行を `>` マーカーではなくハイライト（反転表示）で表現

## 決定

### EditableBuffer → TextBuffer リネーム

EditableBuffer は read-only フラグを持つ汎用バッファ実装となるため、
「Editable」という名前は不適切になる。TextBuffer にリネームする。

### read-only フラグ

TextBuffer に `readOnly` フラグを追加し、`setReadOnly(boolean)` / `isReadOnly()` を実装する。
BufferFacade にも `setReadOnly(boolean)` を公開し、スクリプトからも利用可能にする。

BufferFacade の既存の `checkReadOnly()` がバッファレベルの `isReadOnly()` をチェックするため、
read-only バッファへのユーザー操作による書き込みはブロックされる。

内部更新（completions表示の再構築）は `setReadOnly(false)` → 更新 → `setReadOnly(true)` で行う。
CommandLoop は単一スレッドで逐次実行されるため、この間にユーザー操作が割り込むことはない。

### 選択行ハイライト

Window に `highlightPointLine` フラグを追加する。
このフラグが有効なウィンドウでは、point が存在する行を反転表示でハイライトする。
Emacs の hl-line-mode に相当する汎用的な仕組みとする。

RenderSnapshot の WindowSnapshot にハイライト行情報を追加し、
ScreenRenderer でその行を反転表示する。

## 影響

- EditableBuffer の全参照が TextBuffer に変わる（コンパイルでカバー可能）
- CompletionsModel.formatForDisplay() から `>` マーカーを除去
- read-only フラグはスクリプトからも利用可能な汎用API
- highlightPointLine は将来他のバッファ（grep結果等）でも活用可能
