# ADR 0118: PageUp/PageDown キーバインド追加

## ステータス

承認済み

## コンテキスト

ADR 0078 で導入した `scroll-up` (C-v) / `scroll-down` (M-v) コマンドに対し、
PageUp/PageDown キーでも同じ操作を行えるようにする要望がある。

現状、Lanterna の `KeyType.PageUp` / `KeyType.PageDown` は `KeyStrokeConverter` の
`default` ケースに落ちて `Optional.empty()` を返すため、入力自体が無視されている。

## 決定

### KeyStroke への定数追加

既存の矢印キー定数（`0xF700`-`0xF703`、Unicode Private Use Area）に続いて、
PageUp/PageDown 用の定数を追加する。

```java
public static final int PAGE_UP = 0xF704;
public static final int PAGE_DOWN = 0xF705;
```

`keyCodeToString` では Emacs の内部名に準拠し `<prior>` / `<next>` を使用する。

### KeyStrokeConverter への変換ケース追加

Lanterna の `KeyType.PageUp` → `PAGE_UP`、`KeyType.PageDown` → `PAGE_DOWN` を変換する。

### キーバインド登録

既存の C-v / M-v バインドと同じコマンドを PageDown / PageUp にバインドする。

- `PAGE_DOWN` → `scroll-up`（C-v と同じ。ページを下方向に読み進める）
- `PAGE_UP` → `scroll-down`（M-v と同じ。ページを上方向に戻る）

Emacs の命名規約では `scroll-up` は「ビューを上にスクロールする＝下方向に読み進める」を意味する。

## 影響

- `KeyStroke` に定数 2 つ、`keyCodeToString` に 2 ケース追加
- `KeyStrokeConverter` に 2 ケース追加
- `EditorCore` に `keymap.bind` 2 行追加
- 既存コードへの修正なし（純粋な追加変更）
