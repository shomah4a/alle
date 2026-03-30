# ADR 0090: occur-mode の実装

## ステータス

承認

## コンテキスト

バッファ内の文字列を検索し、マッチした行の一覧を表示する機能（Emacsのoccurに相当）が必要である。
マッチ行一覧から元バッファの該当箇所にジャンプできることで、コード閲覧の効率が向上する。

## 決定

### 基本設計

- TreeDiredModeと同じパターン（MajorMode + モード固有CommandRegistry + Keymap）で実装する
- パッケージ: `io.github.shomah4a.alle.core.mode.modes.occur`
- occurコマンドはグローバルコマンドとして登録する

### バッファ命名規則

- バッファ名は `*Occur <元バッファ名>*` とする
- 同じバッファで複数回occurした場合はバッファ内容を上書きする
- Emacsの `*Occur*` と異なり、元バッファ名を含めることで複数バッファのoccur結果を同時に保持できる

### 表示形式

```
3 lines matching "query" in buffer-name
    10: マッチした行のテキスト
    25: 別のマッチ行
   103: さらに別のマッチ行
```

- ヘッダ行（1行）+ マッチ行
- 1行に複数マッチがあっても行は1回のみ表示
- 行番号は1始まりで表示（ユーザー向け）

### ウィンドウ操作

- occurコマンド実行時にアクティブウィンドウを上下分割し、下側にoccurバッファを表示する
- 実行後のフォーカスはoccurバッファ（下ウィンドウ）に移る
- カーソル移動（C-n/C-p/矢印キー）に連動して上ウィンドウの該当行にジャンプする
- RETで上ウィンドウの該当行にジャンプし、フォーカスも上ウィンドウに移る

### 上ウィンドウへのジャンプ

- OccurModelにsourceBufferNameを保持する
- bufferManagerからバッファを探し、windowTreeからそのバッファを表示しているウィンドウを探す
- ウィンドウが見つからない場合はジャンプをスキップする

### モード固有キーバインド

| キー | コマンド |
|---|---|
| RET | occur-goto-line |
| C-n / ↓ | occur-next-line |
| C-p / ↑ | occur-previous-line |
| q | occur-quit |
| デフォルト | no-op |

### カーソル移動の委譲

occur-next-line / occur-previous-line では、既存のnext-line / previous-lineコマンドをCommandResolver経由で取得・実行し、カーソル移動後にジャンプ処理を行う。移動ロジックの重複を避ける。

## 影響

- EditorCore.java へのグローバルコマンド登録追加
- 新規パッケージ `io.github.shomah4a.alle.core.mode.modes.occur` の追加
- 既存コードの変更は最小限（EditorCoreの初期化メソッドへの追加のみ）
