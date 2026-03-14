# コマンドシステム実装計画

## ステップ

1. ADR-0011作成 ← 完了
2. Command インターフェース + CommandContext 実装・テスト・コミット
3. KeySequence 実装・テスト・コミット
4. Keymap 実装・テスト・コミット
5. KeyResolver 実装（複数Keymapの優先順位付きルックアップ）・テスト・コミット
6. 基本カーソル移動コマンド（forward-char, backward-char）実装・テスト・コミット
7. 安全性評価
