# ADR-0014: 基本編集コマンドの追加

## ステータス

承認済み

## コンテキスト

CommandLoopとSelfInsertCommandにより文字挿入は可能になったが、削除・カーソル移動・改行挿入など基本的な編集操作のコマンドが不足している。
テキスト編集の最低限の操作を一通り揃える必要がある。

## 決定

### 追加コマンド

| コマンド名 | キーバインド | 動作 |
|---|---|---|
| delete-char | C-d | カーソル位置の文字を削除 |
| backward-delete-char | DEL/Backspace | カーソル前の文字を削除 |
| beginning-of-line | C-a | 行頭へ移動 |
| end-of-line | C-e | 行末へ移動 |
| newline | RET | 改行を挿入 |
| kill-line | C-k | カーソルから行末まで削除。行末なら改行を削除。バッファ末尾では何もしない |

### API追加

beginning-of-line、end-of-line、kill-lineの実装にはポイント位置から行インデックスを逆引きするAPIが必要であるため、TextModel/GapTextModelに`lineIndexForOffset(int offset)`を追加する。

改行文字上のポイントはEmacsに倣い「その行の末尾」として扱う。

### kill-lineとkill-ringの関係

kill-lineはEmacsではkill-ringにテキストを蓄積するが、現時点ではkill-ring機構を導入しない。
理由:
- kill-ringはyank (C-y)、yank-pop (M-y) と密結合しており、スコープが大きい
- まず削除動作のみ実装し、kill-ring統合は別途行う

将来kill-ringを導入する際の拡張ポイント:
- CommandContextにkill-ring参照を追加する
- kill-lineコマンドが削除テキストをkill-ringに渡すように変更する
- 連続kill-line実行時のテキスト結合は「前回のコマンドが何であったか」をCommandContextで参照可能にする必要がある

## 帰結

- 挿入・削除・カーソル移動・改行の基本編集操作がコマンド経由で行えるようになる
- kill-ringは技術的負債として認識し、別途ADRを作成して対応する
