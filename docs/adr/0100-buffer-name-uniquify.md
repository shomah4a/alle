# ADR 0100: バッファ名のuniquify機能

## ステータス

承認

## コンテキスト

別ディレクトリの同名ファイルを複数開いた状態で `kill-buffer` や `switch-to-buffer` を実行すると、
バッファ名が同一であるためどちらのバッファが対象か区別できない。

現状の `BufferIO.load()` はファイル名のみ (`Path.getFileName()`) をバッファ名として設定しており、
`BufferManager.findByName()` / `switchTo()` / `remove()` は名前の最初のマッチを返すため、
同名バッファが存在する場合に意図しないバッファが操作される。

Emacs では `uniquify` 機能により、同名バッファが存在する場合にディレクトリパスの情報を付加して
バッファ名を自動的に区別する仕組みがある。

## 決定

Emacs の forward style uniquify と同様の仕組みを導入する。

### バッファ名フォーマット

同名ファイルのディレクトリパスの最長共通プレフィックスを除いた相対パスを使用する:
- `/home/user/project1/src/main.py` → `project1/src/main.py`
- `/home/user/project2/src/main.py` → `project2/src/main.py`

衝突がなくなった場合（一方のバッファが閉じられた場合等）は元のファイル名のみに戻す。

### 実装方式

`Buffer` インターフェースは変更せず、`BufferFacade` に `displayName` フィールドを追加する。

- `BufferFacade.getName()`: `displayName` が設定されていればそちらを返す。未設定なら従来通り
- `BufferFacade.setDisplayName()` / `resetDisplayName()`: uniquify による表示名の設定・リセット
- `BufferNameUniquifier`: ファイルパスを持つバッファをファイル名でグループ化し、
  同名グループが2個以上の場合に表示名を計算するロジックを担う
- `BufferManager.add()` / `remove()` の後に uniquify を実行する
- `BufferManager.recomputeUniquify()`: ファイルパス変更後にコマンドから明示的に呼び出すAPI

### 順序制約

`autoModeMap.resolve()` はバッファ名（ファイル名）から拡張子を取得してメジャーモードを判定する。
この呼び出しは `BufferManager.add()` の前に行われるため、uniquify 前のファイル名で動作する。
この順序は維持すること。

## 結果

- 同名ファイルを複数開いた場合にバッファ名で区別できるようになる
- `kill-buffer` / `switch-to-buffer` の補完候補が一意になる
- `Buffer` インターフェースや `MessageBuffer` への変更が不要
