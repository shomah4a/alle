# ADR 0081: DirectoryLister の返り値を sealed interface DirectoryEntry に変更する

## ステータス

承認済み

## コンテキスト

現在の `DirectoryLister.list()` は `ListIterable<String>` を返し、ディレクトリエントリには末尾 `/` を付与する規約でファイルとディレクトリを区別している。
この文字列ベースの規約は型安全でなく、利用側（`FilePathCompleter`）で末尾 `/` の有無による分岐が必要になっている。

今後 Tree Dired（ツリー表示のディレクトリブラウザ）を実装するにあたり、`DirectoryLister` をより構造化された型で返す必要がある。
補完用途と Dired 用途で別々のインターフェースを設けるよりも、既存の `DirectoryLister` を拡張する方が自然である。

## 決定

`DirectoryLister.list()` の返り値を `ListIterable<DirectoryEntry>` に変更する。

`DirectoryEntry` は sealed interface とし、`File` と `Directory` の 2 つの record で構成する。
フィールドは `path()` のみとする。ファイル名は `path().getFileName()` で導出できるため、`name()` は持たない。
ファイル/ディレクトリの判別は sealed type のパターンマッチで行い、末尾 `/` 規約を廃止する。

```java
public sealed interface DirectoryEntry {
    Path path();

    record File(Path path) implements DirectoryEntry {}
    record Directory(Path path) implements DirectoryEntry {}
}
```

将来的に symlink, hardlink 等のバリアントを追加する可能性がある。

## 影響範囲

- `DirectoryLister` インターフェース (返り値型の変更)
- `DirectoryEntry` 新規作成
- `FilePathCompleter` (DirectoryEntry を使った補完候補生成に書き換え)
- `Main.listDirectory` (実装の書き換え)
- テスト (スタブの書き換え)

## 将来の拡張

Dired 実装時にパーミッション等の属性を `DirectoryEntry` に追加する可能性がある。
