# ADR 0076: Python モードの逆方向インデントサイクル

## ステータス

承認

## コンテキスト

Python モードでは TAB キー連打によるインデントサイクル機能が実装されている（ADR 0061）。
候補リスト `[前行インデント, +4, -4, 0]` を順方向に循環するが、逆方向への循環手段がない。
Shift+Tab で逆方向にサイクルできるようにしたい。

## 決定

### ターミナル入力層

- `KeyStrokeConverter` に Lanterna の `KeyType.ReverseTab` を SHIFT+TAB の KeyStroke に変換する分岐を追加する

### Python keybind ヘルパー

- `keybind.py` に `shift()` ヘルパー関数を追加する

### コマンド実装

- `commands.py` の循環ロジックを方向パラメータ付きの共通関数に抽出する
- 逆方向サイクル用の `python-dedent-line` コマンドを追加する
- 順方向・逆方向で同一の候補リストとグローバル状態を共有する

### キーバインド

- Shift+Tab を `python-dedent-line` にバインドする

### キー表示

- Shift+Tab の `displayString()` は Emacs に合わせて `<backtab>` と表示する
- Tab は `TAB` と表示する（従来は `<9>` と表示されていた）

### インデント候補リスト

- 候補から「前行より深いインデント」(`prev + 4`) を除外する
- 括弧なしで前行より深いインデントにする意味がないため

## 影響

- 既存の TAB インデントサイクルの動作は変わらない
- Shift+Tab が未使用だったため既存キーバインドとの競合はない
