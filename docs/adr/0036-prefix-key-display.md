# ADR 0036: プレフィックスキー表示と processKey の非ブロッキング化

## ステータス

承認

## コンテキスト

プレフィックスキー（C-x 等）を押した際に、エコーエリアにキー入力状態が表示されない。
原因は `CommandLoop.handlePrefix()` が内部で `inputSource.readKeyStroke()` をブロッキング呼び出ししており、
Main.run() のレンダリングが挟まらないため。

入力処理と UI 描画を分離し、将来的なフロントエンド差し替えにも備える。

## 決定

### processKey の非ブロッキング化

- `processKey` は 1 キーにつき 1 回の状態遷移で即座に return する
- プレフィックス状態を `CommandLoop` 内部に保持する
- `handlePrefix` メソッドを廃止し、`processKey` 内で状態に基づいてキーを解決する

### プレフィックス状態の管理

- `@Nullable PendingPrefix` レコードで `Keymap` と表示文字列をまとめて管理
- PrefixBinding に当たったら状態を設定し、`messageBuffer` にキー表示文字列を設定して return
- 次の `processKey` 呼び出しで状態に基づいて解決
- マッチしないキーや C-g で状態をクリア

### KeyStroke の表示文字列化

- `KeyStroke.displayString()` メソッドを追加
- `C-x`、`M-x`、`a` 等のフォーマットを返す

## 影響

- CommandLoop の processKey が非ブロッキングになる
- Main.run() のループ構造（readKey → processKey → render）は変更なし
- handlePrefix 内の readKeyStroke 呼び出しが不要になる
