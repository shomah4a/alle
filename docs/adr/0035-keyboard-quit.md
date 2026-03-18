# ADR 0035: keyboard-quit (C-g)

## ステータス

承認

## コンテキスト

C-g (keyboard-quit) はEmacsにおける基本的な中断操作であり、
マーク解除やプレフィックスキーの中断に使われる。
現状ではミニバッファのキャンセルにのみC-gが使われており、
通常のバッファ編集中やプレフィックスキー途中では何も起きない。

## 決定

### KeyboardQuitCommand の導入

- マークを解除し、エコーエリアに "Quit" を表示する
- `keyboard-quit` コマンドとして CommandRegistry に登録

### Keymap のデフォルト C-g バインド

- `Keymap` に static な quit コマンドフィールドを持たせる
- `Keymap.setQuitCommand(Command)` で設定
- `lookup()` で C-g に明示バインドがない場合、quit コマンドを返す
- 優先順位: 明示バインド → defaultCommand → quitCommand
- これにより全キーマップインスタンス（プレフィックスキーマップ含む）で C-g が自動的に効く

### ミニバッファとの共存

- ミニバッファのローカルキーマップでは C-g が `MinibufferCancelCommand` として明示バインドされている
- CommandLoop.resolveKey() でローカルキーマップが最優先で解決されるため、keyboard-quit との競合は発生しない

## 影響

- Keymap クラスに static フィールド追加
- C-g は慣習的に quit 専用のキーであり、テスト分離のリスクは実質的にない
