# ADR-0064: 補完候補一覧表示とナビゲーション

## ステータス

承認済み

## コンテキスト

ADR-0023 でミニバッファの Tab 補完（最長共通プレフィックス方式）を実装した。
ADR-0023 では「連続 Tab での候補一覧バッファ表示」をスコープ外としていた。

Emacs のように、補完が進まない場合に `*Completions*` バッファで候補一覧を表示し、
候補のナビゲーション・選択を可能にしたい。

### 要件

- 補完が進まなかった場合の再 Tab で候補一覧をウィンドウ分割表示
- 候補一覧内を C-n/C-p でナビゲーション可能
- RET で選択候補を確定
- ミニバッファでの文字入力を継続可能（入力変更時に候補リストを更新）
- 補完候補のカスタマイズは既存の Completer インターフェース経由

## 決定

### CompletionOutcome: 補完結果の構造化

`CompletionResult.resolve()` は String を返すが、候補一覧表示には候補リスト情報が必要。
新たに `CompletionOutcome` sealed interface を導入する。

```java
sealed interface CompletionOutcome {
    record Unique(String value) implements CompletionOutcome {}
    record Partial(String commonPrefix, ListIterable<String> candidates) implements CompletionOutcome {}
    record NoMatch() implements CompletionOutcome {}
}
```

既存の `CompletionResult.resolve()` は維持し、`resolveDetailed()` を追加する。

### CompletionsModel: 候補状態管理

候補リストと選択インデックスを管理するクラス。
テキスト上のカーソル位置ではなくインデックスベースで状態管理する。

- `selectNext()` / `selectPrevious()`: 選択移動
- `getSelectedCandidate()`: 選択中の候補を返す
- `getCandidates()`: 候補リストを返す
- `getSelectedIndex()`: 選択インデックスを返す

### *Completions* バッファ

- `BufferManager` にシステムバッファとして登録する
- `isSystemBuffer() = true` とし、switch-buffer の候補一覧からは除外
- 候補一覧はデフォルトの Emacs 風カラム整形で表示
- 表示整形のカスタマイズは現時点では導入しない（YAGNI）

### 再 Tab 判定

`MinibufferCompleteCommand` に状態を持たせず、`CommandContext.lastCommand()` が
`"minibuffer-complete"` であるかどうかで判定する。

### ウィンドウ分割

ミニバッファアクティブ中のウィンドウ分割はプロジェクト内で初のケースとなる。
`Frame` に `splitWindowBelow(Window target, BufferFacade buffer)` を追加し、
`activeWindow` を変更しない分割操作を提供する。

### ナビゲーション用キーマップ

`*Completions*` 表示中はミニバッファのキーマップにナビゲーション用バインドを追加する。
文字入力時には候補リストを再計算して `*Completions*` バッファを更新する。

- C-n: 次の候補を選択
- C-p: 前の候補を選択
- RET（*Completions* 表示中）: 選択候補で補完確定

### クリーンアップ

ミニバッファ確定(RET)・キャンセル(C-g) 時に `*Completions*` ウィンドウを自動削除する。
ウィンドウが既に閉じられている場合は削除前の存在確認でガードする。

### 変更しないもの

- `Completer` インターフェース
- `InputPrompter` インターフェース（TUI 固有の関心事を混入させない）
- 各 Completer 実装（FilePathCompleter, BufferNameCompleter, CommandNameCompleter）

## 結果

- Tab 補完時に候補一覧が視覚的に確認できるようになる
- 候補のナビゲーション・選択が可能になる
- 既存の Completer インターフェースがそのままカスタマイズポイントとして機能する
