# ADR-0043: ミニバッファ ヒストリ機能

## ステータス
承認済み

## コンテキスト
find-file, save-buffer などのファイルパス入力ミニバッファで、過去に確定した入力を再利用できるヒストリ機能が必要である。Emacsと同様に M-p / M-n / Arrow Up / Arrow Down でヒストリをナビゲートできるようにする。

## 決定

### データモデル
- `InputHistory`: 確定入力の履歴を保持する純粋なデータストア
  - `add(String entry)`: 履歴に追加。空文字は無視、重複は最新位置に移動
  - `get(int index)`: 指定インデックスの履歴を取得（0が最古）
  - `size()`: 履歴サイズ
  - 最大サイズ100。超過時は古いものから削除
  - Eclipse Collections の MutableList で保持

- `HistoryNavigator`: ナビゲーション状態を持つセッションスコープのオブジェクト
  - プロンプトセッション開始時に `new HistoryNavigator(InputHistory, String originalInput)` で生成
  - `previous()`: 1つ前の履歴を返す。先頭なら先頭のまま
  - `next()`: 1つ次の履歴を返す。末尾を超えたら元入力を返す
  - いずれも `Optional<String>` を返し、カーソルが動かなかった場合は empty

### 配置
- `InputHistory`, `HistoryNavigator` は `alle-core` の `input` パッケージに配置
- `InputHistory` はファイルパスピッカー全体で共有（コマンドごとに分けない）

### InputPrompter への統合
- `InputPrompter` に `prompt(String message, String initialValue, Completer completer, InputHistory history)` オーバーロードを追加
- default実装はhistoryなしのpromptに委譲（後方互換）

### キーバインド
ミニバッファのローカルキーマップに以下を追加（historyが提供された場合のみ）:
- `M-p`, `Arrow Up` → 前のヒストリ
- `M-n`, `Arrow Down` → 次のヒストリ

### 共有方法
- `EditorCore.createCommandRegistry` で `InputHistory` インスタンスを1つ生成
- `FindFileCommand` と `SaveBufferCommand` のコンストラクタに注入
- 確定時に `InputHistory.add()` を呼ぶのは `MinibufferInputPrompter` の `MinibufferConfirmCommand` 側で行う

## 結果
- ヒストリデータ（InputHistory）とナビゲーション状態（HistoryNavigator）が分離され、ステートフルさがセッションスコープに限定される
- ファイルパス入力の再利用が可能になり、操作効率が向上する
- 将来的に M-x やバッファ切り替え等にも同じ仕組みを適用可能
