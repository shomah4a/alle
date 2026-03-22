# ADR-0056: Markdown Modeのスタイリング構文拡充

## ステータス

承認

## コンテキスト

ADR-0028 で Markdown Mode の初期スタイリング（見出し、インラインコード、太字、斜体、リンク、リストマーカー）を導入した。
ADR-0055 で RegionMatch によるマルチラインスタイリングと styling パッケージへのリネームを行った。

一般的な Markdown 構文のカバレッジを高めるため、未対応の構文を追加する。

## 決定

### 追加した構文

| 構文 | ルール種別 | Face |
|------|-----------|------|
| コードブロック (` ``` `) | RegionMatch | CODE |
| HTMLコメント (`<!-- -->`) | RegionMatch | COMMENT |
| 水平線 (`---`, `***`, `___`) | LineMatch | COMMENT |
| 参照リンク定義 (`[ref]: url`) | LineMatch | LINK |
| 引用 (`> text`) | LineMatch | STRING |
| 取り消し線 (`~~text~~`) | PatternMatch | STRIKETHROUGH_FACE |
| 画像リンク (`![alt](url)`) | PatternMatch | LINK |
| 参照リンク (`[text][ref]`) | PatternMatch | LINK |
| タスクリスト (`- [ ]`, `- [x]`) | PatternMatch | LIST_MARKER |
| テーブル区切り行 (`\|---\|---\|`) | LineMatch | TABLE |
| テーブルパイプ記号 (`\|`) | PatternMatch | TABLE |

### 新設した Face / FaceAttribute

- `Face.TABLE`: テーブル用（cyan）
- `Face.STRIKETHROUGH_FACE`: 取り消し線用（default + STRIKETHROUGH属性）
- `FaceAttribute.STRIKETHROUGH`: SGR.CROSSED_OUT にマッピング

### Face.COMMENT の色変更

`white_bright` → `black_bright`（灰色）に変更。
`white_bright` はターミナルのデフォルト文字色と区別がつかない場合があったため。

### ルール優先順位

ルールは定義順に評価され、先にマッチしたルールが優先される。
RegionMatch（コードブロック、HTMLコメント）を最優先とし、コードブロック内やHTMLコメント内では他のルールが無視される。

## 帰結

- emacs markdown-mode 相当の主要な構文をカバーした
- 今後のモード追加時にも同じルール定義パターンで構文を追加できる
