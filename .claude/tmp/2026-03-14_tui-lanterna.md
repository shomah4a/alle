# TUI描画層（Lanterna）追加作業計画

## 作業手順

1. [x] ブランチ作成 (feature/tui-lanterna)
2. [x] ADR作成 (0015-tui-lanterna.md)
3. [ ] alle-tuiモジュール作成、Lanterna依存追加 → コミット
4. [ ] KeyStrokeConverter（Lanterna→alle変換）+ テスト → コミット
5. [ ] TerminalInputSource実装（shutdownフラグ付き）→ コミット
6. [ ] ScreenRenderer（最小限: バッファ内容＋カーソル表示）→ コミット
7. [ ] Main（try-finallyでの安全な初期化・終了、C-qで終了）→ コミット
8. [ ] 安全性評価
