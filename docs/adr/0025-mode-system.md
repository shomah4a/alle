# ADR 0025: メジャーモード・マイナーモードシステムの導入

## ステータス

承認

## コンテキスト

エディタにモードの概念がなく、バッファのファイルタイプに応じたキーバインドや振る舞いの切り替えができない。
Emacs のメジャーモード・マイナーモードに相当する仕組みを導入する。

## 決定

### メジャーモード
- `MajorMode` インターフェース: `name()`, `keymap()` (Optional<Keymap>)
- バッファに1つだけ紐づく
- デフォルトは `TextMode`（空キーマップ）
- Buffer のコンストラクタでデフォルトモードを設定し、生成箇所の漏れを防止

### マイナーモード
- `MinorMode` インターフェース: `name()`, `keymap()` (Optional<Keymap>)
- バッファに複数紐づく（MutableList<MinorMode>）
- 返り値は ListIterable<MinorMode>

### キーマップ解決順序
1. バッファローカルキーマップ（ミニバッファ用、現行のまま）
2. マイナーモードキーマップ（後から有効にしたものが優先）
3. メジャーモードキーマップ
4. グローバルキーマップ（KeyResolver）

CommandLoop.resolveKey でバッファからモードキーマップを取得して探索する。
KeyResolver は変更しない。
プレフィックスキーマップ内ではモードキーマップは不参照（Emacs 準拠）。

### AutoModeMap
- 拡張子 → Supplier<MajorMode> のマッピング
- FindFileCommand でファイルオープン時にモードを自動設定

### モードライン表示
- モード名を `(Text)` のような形式で表示

## 影響

- Buffer にメジャーモード・マイナーモードフィールドが追加される
- CommandLoop.resolveKey のキーマップ解決順序が拡張される
- ScreenRenderer のモードラインにモード名が追加される
- FindFileCommand に AutoModeMap が注入される
