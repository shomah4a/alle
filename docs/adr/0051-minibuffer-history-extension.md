# ADR-0051: ミニバッファヒストリの拡張適用

## ステータス
承認済み

## コンテキスト
ADR-0043 で導入したミニバッファヒストリ機能は、現在 find-file と save-buffer のファイルパス入力にのみ適用されている。switch-to-buffer (C-x b)、execute-command (M-x)、kill-buffer (C-x k) でも過去の入力を再利用したい。

## 決定

### ヒストリインスタンスの分類
- `bufferHistory`: switch-to-buffer と kill-buffer で共有する。バッファ名入力という同一ドメインであり、一方で操作したバッファ名が他方で再利用できるのは自然な挙動。Emacsの挙動とも一致する
- `commandHistory`: execute-command 専用。コマンド名というドメインが異なるため分離する

### 適用方法
ADR-0043 で確立したパターンをそのまま踏襲する:
- 各コマンドのコンストラクタに `InputHistory` パラメータを追加
- `prompt(message, "", completer, history)` で呼び出す
- `EditorCore.createCommandRegistry` でインスタンスを生成し注入

### 対象コマンド
| コマンド | ヒストリ |
|---------|---------|
| SwitchBufferCommand | bufferHistory |
| KillBufferCommand | bufferHistory (共有) |
| ExecuteCommandCommand | commandHistory |

## 結果
- 全てのミニバッファ入力でヒストリナビゲーション (M-p / M-n) が利用可能になる
- 既存のInputHistory/HistoryNavigator基盤をそのまま活用でき、新たなアーキテクチャ変更は不要
