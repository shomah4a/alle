# ADR-0050: Error Prone警告のエラー昇格と-Werror導入

## ステータス

accepted

## コンテキスト

Error Proneによるコンパイル時チェックで10件のwarningが出ている。
warningのまま放置すると新たなwarningが蓄積し、コード品質が劣化する。

## 決定

1. 既存のError Prone warningをすべて修正する
2. javacの`-Werror`オプションを追加し、javac警告（Error Proneのwarningを含む）をビルドエラーにする
3. deprecation APIの利用も合わせて修正する

## 修正対象

- StringSplitter (2箇所): `String.lines()` で置換
- SuperCallToObjectMethod (2箇所): equalsでは`false`に、hashCodeでは`System.identityHashCode(this)`に置換
- UnusedVariable (3箇所): 未使用の変数・フィールド・パラメータを削除
- InvalidParam (1箇所): Javadocの不正なパラメータ名を修正
- ArrayRecordComponent (1箇所): `SGR[]` を `ImmutableList<SGR>` に変更
- ReferenceEquality (1箇所): `equals()` による比較に変更
- deprecated API (TextCharacter.fromString): 非推奨メソッドの代替に変更

## 結果

- コンパイル時に警告が即座にエラーとなり、品質劣化を防止できる
- ArrayRecordComponentの修正はalle-tuiモジュール内で完結する
- Guava依存は追加しない（StringSplitterは`String.lines()`で対応）
