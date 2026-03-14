# ADR-0015: NullAwayチェックの全ファイル適用

## ステータス

承認済み

## コンテキスト

NullAwayの設定で `onlyNullMarked = true` が指定されていたが、`@NullMarked` アノテーションを持つ `package-info.java` や `module-info.java` が一つも存在しなかった。そのためNullAwayのチェック対象がゼロとなり、nullnessチェックが実質的に無効化されていた。

## 検討した選択肢

### 1. `onlyNullMarked = false` に変更

- build.gradle.kts の1行変更で全ファイルがチェック対象になる
- `package-info.java` や `module-info.java` の追加が不要
- パッケージ追加時に `package-info.java` を忘れるリスクがない

### 2. 各パッケージに `package-info.java` を追加して `@NullMarked` を宣言

- 明示的だが8パッケージ分のファイル作成が必要
- パッケージ追加時に忘れるリスクがある

### 3. `module-info.java` を作成して `@NullMarked` を一括適用

- モジュールごとに1ファイルで済む
- ただしJava Module System（JPMS）の導入が伴い、影響範囲が大きい

## 決定

選択肢1を採用する。`onlyNullMarked = false` に変更し、全Javaファイルをチェック対象とする。

リポジトリ内のコードはデフォルトnon-nullとし、nullableな箇所には `@Nullable` アノテーションを付与する。

## 補足

外部ライブラリ（アノテーション未指定）については、NullAwayの楽観的デフォルト（返り値はnon-null、引数はnullable）がそのまま適用される。JSpecify対応済みのライブラリについては `AcknowledgeRestrictiveAnnotations` で正確なチェックが可能。
