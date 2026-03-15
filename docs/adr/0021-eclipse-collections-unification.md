# ADR-0021: java.util コレクションの Eclipse Collections 統一

## ステータス

承認済み

## コンテキスト

ADR-0010 で Eclipse Collections の採用を決定したが、初期実装や後続の機能追加で
java.util の EnumSet, Set, HashMap, Map が残存している。
型レベルでの mutability チェックというプロジェクトルールを徹底するため、
残存する java.util コレクションを Eclipse Collections に統一する。

## 決定

java.util のコレクション（Set, EnumSet, Map, HashMap）を全て Eclipse Collections に置き換える。

### 対象

- `KeyStroke.java`: record component `Set<Modifier>` -> `ImmutableSet<Modifier>`
- `KeyStrokeConverter.java`: `EnumSet<Modifier>` -> `MutableSet<Modifier>` + `toImmutable()`
- `ScreenRenderer.java`: `Set<Character.UnicodeBlock>` -> `ImmutableSet<Character.UnicodeBlock>`
- テストコード: `HashMap`/`Map` -> `MutableMap`

### トレードオフ

- EnumSet のビットマスク最適化が失われるが、Modifier は 3 要素なので影響は無視できる

## 結果

- java.util のコレクションクラスがプロジェクトから除去される
- 型レベルでの mutability 管理が徹底される
