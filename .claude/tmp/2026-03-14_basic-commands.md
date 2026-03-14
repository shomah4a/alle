# 基本編集コマンド追加作業計画

## 作業手順

1. [x] ブランチ作成 (feature/basic-commands)
2. [x] ADR作成 (0014-basic-commands.md)
3. [ ] TextModel/GapTextModel に lineIndexForOffset 追加 + テスト → コミット
4. [ ] delete-char, backward-delete-char 実装 + テスト → コミット
5. [ ] newline 実装 + テスト → コミット
6. [ ] beginning-of-line, end-of-line 実装 + テスト → コミット
7. [ ] kill-line 実装 + テスト → コミット
8. [ ] 安全性評価

## 設計判断

- lineIndexForOffset: 改行文字上のポイントはその行の末尾として扱う
- kill-line: kill-ringなしで削除のみ実装
