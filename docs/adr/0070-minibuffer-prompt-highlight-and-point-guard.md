# ADR 0070: ミニバッファプロンプトのハイライトとカーソル進入禁止

## ステータス
Accepted

## コンテキスト
find-file等のミニバッファ入力時にプロンプト文字列（例: "Find file: "）が通常テキストと同じ見た目で表示されるため、視覚的に目立たない。
また、カーソルがプロンプト領域に移動できてしまい、ユーザー体験が損なわれる。

現在、プロンプト領域の編集はread-onlyプロパティで防止しているが、カーソル移動は制限されていない。

## 決定

### 1. TextPropertyStoreにpointGuard属性を追加する
- read-onlyとは独立した概念として「カーソル進入禁止領域（pointGuard）」を導入する
- TextPropertyStore内ではread-onlyのEntryリストとpointGuardのEntryリストを分離して管理する
  - 各属性の操作が互いに干渉しない
  - 将来ある点の全プロパティが必要になった場合は横断集約メソッドで対応可能
- APIはread-onlyと同じパターン: putPointGuard / removePointGuard / isPointGuard
- テキスト挿入・削除時の範囲自動追従はread-onlyと同じ仕組みを適用する

### 2. Window側でpointGuardを尊重する
- setPoint / getPoint でバッファのpointGuardをチェックし、ガード範囲内への移動をクランプする
- 個別のカーソル移動コマンドの修正は不要（Window側で一元的に制御）

### 3. プロンプトのハイライト表示は別タスクとする
- TextPropertyStoreにface（表示スタイル）属性を追加し、汎用的なハイライト機構として実装する予定
- pointGuardからの間接的な計算ではなく、face属性として独立して管理する

## 理由

### read-onlyとpointGuardの分離
- read-only: テキストの編集を禁止する（書き込み制御）
- pointGuard: カーソルの進入を禁止する（移動制御）
- 用途が異なるため独立して設定できるべき

### Bufferレベルでの管理
- 進入禁止領域はテキスト側の性質であり、Windowの状態ではない
- 同一バッファを複数Windowで開いた場合にWindow毎に異なるのは不自然
- eshell等の将来的なユースケースではバッファ内に複数の進入禁止範囲が点在する

### 属性種別ごとのリスト分離
- putReadOnlyがpointGuardエントリを巻き込むリスクがない
- 各属性の追加・除去が互いに干渉しない
- 現状「ある点のプロパティ一覧」を返すユースケースがなく、個別属性チェックのみ
