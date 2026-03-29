# ADR 0080: 補完候補リストのソート

## ステータス

承認済み

## コンテキスト

find-file などの補完候補リストがソートされておらず、ユーザーにとって見づらい状態だった。

## 決定

- `Completer` インターフェースに `sortedComplete` デフォルトメソッドを追加する
  - `complete` の結果を `label` の自然順（`String::compareTo`）でソートして返す
  - `@FunctionalInterface` は維持される（デフォルトメソッド1つ追加のみ）
  - オーバーライドは想定しない旨をJavadocで示す
- `MinibufferInputPrompter` 内の表示に関わる呼び出し箇所（2箇所）で `sortedComplete` を使用する
  - `MinibufferCompleteCommand`（TAB補完時）
  - `MinibufferSelfInsertCommand`（入力中の候補更新時）
- 順番に意味のない箇所（`MinibufferConfirmCommand` 内の候補数判定）は `complete` のまま

## 結果

補完候補がソート済みで表示され、ユーザーが目的の候補を見つけやすくなる。
