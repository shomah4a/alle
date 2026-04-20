# ADR 0126: 矩形編集コマンド（rectangle 系）

## ステータス

承認

## コンテキスト

Emacs における矩形 (rectangle) 操作は、リージョンを行開始行〜終了行の間の「左カラム〜右カラム」という矩形範囲として解釈し、その範囲のみを対象とした削除・コピー・挿入・置換を行う。本エディタにはこれに相当する機能がなく、矩形単位の編集を行う手段がない。

本 ADR では v1 として 7 コマンドを導入する。

## 決定

### コマンド

Emacs 互換のキーバインドで以下 7 コマンドを導入する。すべて `C-x r` プレフィックス配下。

| コマンド名 | キー | 概要 | keepsRegionActive |
| --- | --- | --- | --- |
| `kill-rectangle` | `C-x r k` | 矩形を削除して killed-rectangle に保存 | false |
| `delete-rectangle` | `C-x r d` | 矩形を削除（保存しない） | false |
| `copy-rectangle-as-kill` | `C-x r M-w` | 矩形を保存（削除しない） | false |
| `yank-rectangle` | `C-x r y` | 直近の killed-rectangle を point 位置に挿入 | false |
| `open-rectangle` | `C-x r o` | 矩形サイズの空白を挿入し、矩形右側の既存テキストを右に押し出す | false |
| `clear-rectangle` | `C-x r c` | 矩形内容を空白で埋める（右側テキストは動かさない） | false |
| `string-rectangle` | `C-x r t` | プロンプトで文字列を受け取り、各行の矩形範囲を置き換える | 実装で座標をキャプチャ後 clear してよい |

### v1 スコープ外

以下は別タスクとして `docs/tasks.md` に登録する:

- `rectangle-number-lines` (`C-x r N`): プレフィックス引数（開始番号・フォーマット文字列）の基盤が必要
- `string-insert-rectangle`: 文字列を矩形として行毎に挿入
- CUA rectangle-mark-mode: 矩形選択モード

### 矩形の座標系

矩形は `[leftCol, rightCol)` × `[startLine, endLine]` で表現する。

- 列は **表示カラム (display column)**。タブは `TAB_WIDTH` 設定に従って展開、全角文字は 2 カラム。
- `(mark, point)` から導出:
  - `startLine = min(lineOf(mark), lineOf(point))`
  - `endLine   = max(lineOf(mark), lineOf(point))`
  - `leftCol   = min(columnOf(mark), columnOf(point))`
  - `rightCol  = max(columnOf(mark), columnOf(point))`

### 境界が全角文字/タブの中央に落ちる場合

統一ルール: 境界が全角文字やタブの途中に落ちる場合、その文字を一旦スペース展開してから矩形を切り出す（= 元のタブ/全角文字は破壊される）。Emacs の挙動と整合。

具体例:

- タブ（`TAB_WIDTH=8`）1 文字だけの行で `[leftCol=3, rightCol=5)` を切る → タブをスペース 8 個に展開 → `[3, 5)` の 2 スペースを切り出し、残り 6 スペースが行に残る。
- 全角 `あ`（2 カラム）1 文字の行で `[leftCol=0, rightCol=1)` を切る → `あ` をスペース 2 個に展開 → `[0, 1)` の 1 スペースを切り出し、残り 1 スペースが行に残る。

挿入系で目的カラムに届かない行は末尾にスペースを補填して目的カラムに揃える。

### killed-rectangle の保存先

- `RectangleKillRing` クラスを新設し、行毎の文字列 `ListIterable<String>` として保存する。
- v1 は直近 1 件のみ保持（サイズ 1 相当）。将来の履歴拡張を見越して Ring 命名を維持し、`KillRing` と並べて見通しを良くする。
- **`CommandContext` record には追加しない**。理由: `new CommandContext(...)` を直接書いている箇所が本体 1（`CommandLoop`）+ テスト 10 ファイルあり、フィールド追加は破壊的影響が大きい。代わりに `CommandLoop` のコンストラクタで受け取り、矩形コマンドのインスタンスにも同じ参照を注入する（`EditorCore` が両方に同じインスタンスを渡す）。
- 既存 `KillRing` は文字列としての kill/yank に使われており、矩形と混ぜると `yank` と `yank-rectangle` の挙動が混乱するため分離する。Emacs も `killed-rectangle` を独立変数として持つ。

### カラム ↔ コードポイントオフセット変換の集約

現状、類似ロジックが散在している:

- `NextLineCommand#computeCpForColumn`（package-private static）
- `DisplayWidthUtil#snapColumnToCharBoundary`（カラム → カラム）
- `DisplayWidthUtil#computeColumnForOffset`（オフセット → カラム）

「やり方は一つ」の観点から、`NextLineCommand#computeCpForColumn` を `DisplayWidthUtil#computeOffsetForColumn` として移動し public static 化する。NextLineCommand と矩形コマンドは共通 API を使う。挙動変更を伴わないリファクタとして先行して実施する。

### undo 単位

- **同期コマンド 6 本（kill/delete/copy/yank/open/clear）**: `CommandLoop#handleEntry` が `UndoManager#withTransaction` で自動ラップするため、1 コマンド = 1 undo。追加対応不要。
- **`string-rectangle`（非同期）**: `InputPrompter#prompt` を介するため、`CommandLoop.withTransaction` は `prompt` を返した時点で閉じる。プロンプト完了後のコールバック内で `BufferFacade#atomicOperation` を取得し、その内側で `UndoManager#withTransaction` を使い 1 undo にまとめる。

### `string-rectangle` の詳細

- `InputPrompter` で文字列を受け取る（`find-file` / `query-replace` と同じ非同期パターン）。
- 矩形座標 `(startLine, leftCol, endLine, rightCol)` を **プロンプト開始前に確定** してクロージャにキャプチャする。これによりプロンプト完了時に `CommandLoop#handleEntry` が自動で `clearMark` を呼んでも、またプロンプト入力中にユーザーが mark/point を操作しても、矩形編集は開始時点の範囲で実行される。
- 空文字列入力時は **矩形を削除**（`delete-rectangle` 相当、右側テキストは詰める）。`clear-rectangle`（空白で埋めるが右側は動かさない）とは異なる。
- 非空入力時は各行の矩形範囲をその文字列で置き換える。入力文字列のカラム幅が元の矩形幅と異なっても、行右側は詰める／押し出す。

### 空リージョン時の挙動

mark が未設定の場合はエコーエリアに `"No region active"` を出し、編集は行わない（既存 `indent-region` と整合）。

### キーバインド

EditorCore のグローバルキーマップに `C-x r` プレフィックスマップを追加する。`Keymap` はプレフィックス配下で Meta 修飾付き KeyStroke をサポートしており、`C-x r M-w` も直接バインド可能。

```
C-x r k     kill-rectangle
C-x r d     delete-rectangle
C-x r y     yank-rectangle
C-x r o     open-rectangle
C-x r c     clear-rectangle
C-x r t     string-rectangle
C-x r M-w   copy-rectangle-as-kill
```

### 実装構造

- `Rectangle` — 矩形座標を表す値クラス
- `RectangleGeometry` — `(mark, point)` から矩形を導出、行毎スライス/挿入のコアロジック
- `RectangleKillRing` — killed-rectangle の保存先
- `KillRectangleCommand` / `DeleteRectangleCommand` / `CopyRectangleAsKillCommand` / `YankRectangleCommand` / `OpenRectangleCommand` / `ClearRectangleCommand` / `StringRectangleCommand`

## 影響

- `command.commands` パッケージにクラスが追加される。
- `DisplayWidthUtil` に `computeOffsetForColumn` が追加され、`NextLineCommand` は新 API を呼ぶよう変更される（挙動変更なし）。
- `CommandContext` は変更しない。
- `EditorCore` に 7 コマンドとキーバインドが追加される。
- `CommandLoop` のコンストラクタに `RectangleKillRing` パラメータが追加される。

## 代替案

### killed-rectangle を既存 `KillRing` に混ぜて格納

却下。Emacs の挙動（`yank` と `yank-rectangle` の分離）と整合せず、実装上も型で区別する必要がある。

### `RectangleKillRing` を `CommandContext` に追加

却下。破壊的影響が大きい（直接生成箇所 11 ファイル）。`CommandLoop` 経由注入で機能面は等価に実現できる。

### 列をコードポイント単位で扱う

却下。Emacs 互換性・タブ/全角文字のユーザー体験の観点で表示カラム単位が妥当。

## 参考

- Emacs Manual: [Rectangles](https://www.gnu.org/software/emacs/manual/html_node/emacs/Rectangles.html)
- ADR 0125 (query-replace): 非同期コマンドと undo の関係、`InputPrompter` の使い方
