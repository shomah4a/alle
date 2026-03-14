# ADR-0010: Eclipse Collectionsの導入

## ステータス

承認済み

## コンテキスト

Java標準の`java.util.List`や`java.util.Map`は、可変（mutable）・不変（immutable）の区別が型レベルで表現されない。
`Collections.unmodifiableList()`等のラッパーは実行時にのみ不変性を保証し、コンパイル時には可変リストと同じ`List`型として見える。
これにより、APIの利用者がリストの変更可否を型から判断できず、変更時のリスクが増大する。

## 決定

Eclipse Collections 13.0.0を導入し、コレクション型のmutabilityを型で明示する。

### 型の使い分け方針

- **privateフィールド**: `MutableList`, `MutableMap`等の可変型を使用する
- **publicメソッドの戻り値・引数**: `ListIterable`, `RichIterable`等の読み取り専用型を使用する
- `java.util.List`, `java.util.Map`等のJava標準コレクション型は使用しない

### ダウンキャストに対する方針

`ListIterable`で返した参照を呼び出し側が`MutableList`にダウンキャストすることは、呼び出し側の責任とする。
型で明示されている以上、実行時の防御（`asUnmodifiable()`等）は設けない。

### Eclipse Collectionsのイディオム

- リスト生成: `Lists.mutable.empty()`, `Lists.mutable.of(...)`
- 検索: `detect()` （`stream().filter().findFirst()`の代替）
- 変換: `collect()`, `select()`, `reject()`等のEclipse Collections APIを優先的に使用する

## 帰結

- コレクションのmutabilityがコンパイル時に型として明示され、APIの意図が明確になる
- 全サブプロジェクトにEclipse Collectionsの依存が追加される
- 既存のjava.util系コレクションを段階的にEclipse Collections型に移行する必要がある
