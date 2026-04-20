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

### 境界が全角文字/タブの中央に落ちる場合（非破壊の外側スナップ）

**Emacs の `move-to-column FORCE` 式のスペース展開は採用しない**。Emacs はタブや全角を破壊的にスペース展開するが、本実装は元のテキストを壊さない方針を採る。

挙動は操作種別で分ける:

- **kill / delete / copy-rectangle-as-kill / clear-rectangle**: 跨ぎ文字を**矩形に含める**（広げる方向）。
  - 左境界がタブ/全角の中央に落ちる → その文字の左端に広げる (round down)
  - 右境界がタブ/全角の中央に落ちる → その文字の右端に広げる (round up)
- **yank-rectangle / open-rectangle**: 挿入位置が跨ぎ文字の中央に落ちたら**その文字の後ろに挿入**（右スナップ = round up）。

具体例:

- `"a\tb"` (TAB=8) の col[0, 3) を `kill-rectangle` → 右境界 col 3 はタブ中央 → 広げて col 8 → 切り出し `"a\t"`、削除後 `"b"`
- `"aあb"` の col[0, 2) を `kill-rectangle` → 右境界 col 2 はあの中央 → 広げて col 3 → 切り出し `"aあ"`、削除後 `"b"`
- `"a\tb"` の col 3（タブ中央）に `"XY"` を `yank-rectangle` → タブの後（col 8）に挿入 → `"a\tXYb"`

**`clear-rectangle` の幅**: 跨ぎ文字を含めて削除した後、実矩形カラム幅分のスペースを挿入する。これにより右側テキストの視覚位置が保たれる。

**行が `leftCol` に届かない場合**: 挿入系は末尾にスペースを padding してから挿入する（Emacs と同じ）。

### 幅 0 矩形の扱い

`leftCol == rightCol` の幅 0 矩形は、`kill` / `delete` / `copy-as-kill` / `clear` / `open` / `yank` では **no-op**（早期 return）。undo 履歴にも積まない。ユーザ認知上「範囲が選択されていない」と等価。

`string-rectangle` は空文字列入力時のみ `delete` 同様幅 0 no-op とし、非空文字列入力時は各行の指定カラムに文字列を挿入する（Emacs 挙動）。

### kill/delete/clear/string-rectangle 完了後の point 位置

各コマンドの完了後、point を**矩形左上** (`startLine, leftCol` を round down したバッファオフセット) に移動する。これにより削除で元の point がバッファ長外に飛ばされる不具合を防ぐ。

`copy-rectangle-as-kill` / `open-rectangle` / `yank-rectangle` は point を変更しない（編集前の位置を維持）。

### killed-rectangle の保存先

- `RectangleKillRing` クラスを新設し、行毎の文字列 `ListIterable<String>` として保存する。
- v1 は直近 1 件のみ保持（サイズ 1 相当）。将来の履歴拡張を見越して Ring 命名を維持し、`KillRing` と並べて見通しを良くする。
- **`CommandContext` record には追加しない**。理由: `new CommandContext(...)` を直接書いている箇所が本体 1（`CommandLoop`）+ テスト 10 ファイルあり、フィールド追加は破壊的影響が大きい。代わりに `EditorCore` の `createCommandRegistry` 内で `RectangleKillRing` を生成し、kill / copy-as-kill / yank の 3 コマンドに共通注入する。
- 既存 `KillRing` は文字列としての kill/yank に使われており、矩形と混ぜると `yank` と `yank-rectangle` の挙動が混乱するため分離する。Emacs も `killed-rectangle` を独立変数として持つ。

### カラム ↔ コードポイントオフセット変換の集約

`NextLineCommand#computeCpForColumn`（package-private）を `DisplayWidthUtil#computeOffsetForColumn`（public static）に移動し、NextLineCommand / PreviousLineCommand / 矩形コマンド群が共通 API を使う。挙動変更を伴わないリファクタ。

### undo 単位

- **同期コマンド 6 本（kill/delete/copy/yank/open/clear）**: `CommandLoop#handleEntry` が `UndoManager#withTransaction` で自動ラップするため、1 コマンド = 1 undo。追加対応不要。
- **`string-rectangle`（非同期）**: `InputPrompter#prompt` を介するため、`CommandLoop.withTransaction` は `prompt` を返した時点で閉じる。プロンプト完了後のコールバック内で `BufferFacade#atomicOperation` を取得し、その内側で `UndoManager#withTransaction` を使い 1 undo にまとめる。

### `string-rectangle` の詳細

- `InputPrompter` で文字列を受け取る（`find-file` / `query-replace` と同じ非同期パターン）。
- 矩形座標 `(startLine, leftCol, endLine, rightCol)` を **プロンプト開始前に確定** してクロージャにキャプチャする。これによりプロンプト完了時に `CommandLoop#handleEntry` が自動で `clearMark` を呼んでも、またプロンプト入力中にユーザーが mark/point を操作しても、矩形編集は開始時点の範囲で実行される。
- 空文字列入力時は **矩形を削除**（`delete-rectangle` 相当、右側テキストは詰める）。`clear-rectangle`（空白で埋めるが右側は動かさない）とは異なる。
- 非空入力時は各行の矩形範囲をその文字列で置き換える。

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
- `RectangleGeometry` — 矩形ジオメトリ計算と高水準編集 API
  - `columnRoundDown` / `columnRoundUp` — 列 → コードポイントオフセット変換（左/右スナップ）
  - `cpRangeForLine` — 各行の矩形範囲（外側スナップ済み）
  - `extractRectangle` / `deleteRectangle` / `replaceRectangle` / `clearRectangle` / `openRectangle` / `insertRectangleAtPoint`
  - `topLeftOffset` — point 再配置用
- `RectangleKillRing` — killed-rectangle の保存先
- `KillRectangleCommand` / `DeleteRectangleCommand` / `CopyRectangleAsKillCommand` / `YankRectangleCommand` / `OpenRectangleCommand` / `ClearRectangleCommand` / `StringRectangleCommand`

## 影響

- `command.commands` パッケージにクラスが追加される。
- `DisplayWidthUtil` に `computeOffsetForColumn` が追加され、`NextLineCommand` / `PreviousLineCommand` は新 API を呼ぶよう変更される（挙動変更なし）。
- `CommandContext` は変更しない。
- `EditorCore` に 7 コマンドとキーバインドが追加される。`createCommandRegistry` 内で `RectangleKillRing` を生成し 3 コマンドに共通注入する。
- `CommandLoop` は変更しない。

## 代替案

### A. Emacs 互換の破壊的スペース展開

却下。新規実装で Emacs の破壊挙動を引き継ぐのは妥協。タブインデントのコードを編集する際に意図せずタブがスペースに壊れるため、ユーザ体験が悪い。

### B. 文字境界にスナップ（跨ぎ文字を矩形から外す）

却下。ユーザが指定した列範囲より小さく取られるため、矩形の意味が弱くなる。

### C. 跨ぎ検出時にエラー拒否

却下。タブインデントや全角を含むファイルでほぼ常に操作不能になり実用に耐えない。

### D. 文字単位矩形（列概念を放棄）

却下。VSCode 等のモダンエディタに近いが、「矩形」の列概念が崩れ、既存 Emacs ユーザの直感と乖離する。

### E. 外側スナップ（採用）

kill/delete 系は跨ぎ文字を含めて広げ、yank/open 系は跨ぎ文字の後ろに挿入する。破壊なし + 列概念維持。

### killed-rectangle を既存 `KillRing` に混ぜて格納

却下。`yank` と `yank-rectangle` の挙動が混ざり、型で区別する必要も生じる。

### `RectangleKillRing` を `CommandContext` に追加

却下。`new CommandContext(...)` 直接生成箇所が 11 ファイルに及ぶため破壊的影響が大きい。`EditorCore` で直接注入する方が影響範囲が小さい。

## 参考

- Emacs Manual: [Rectangles](https://www.gnu.org/software/emacs/manual/html_node/emacs/Rectangles.html)
- ADR 0125 (query-replace): 非同期コマンドと undo の関係、`InputPrompter` の使い方
