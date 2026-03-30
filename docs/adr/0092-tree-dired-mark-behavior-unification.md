# ADR 0092: Tree Dired mark/unmark/toggle の挙動統一

## ステータス

採用

## コンテキスト

ADR 0083 で導入した tree-dired の mark(m), unmark(u), toggle-mark(t) コマンドは、以下のように挙動が不統一である。

| | mark (m) | unmark (u) | toggle (t) |
|---|---|---|---|
| 単一行後の次行移動 | あり | あり | なし |
| リージョン対応 | なし | なし | あり |

ユーザーの操作感として、3つのコマンドが同じパターンで動作することが望ましい:
- リージョンなし: 1行処理 → 次行移動
- リージョンあり: 範囲内を一括処理

## 決定

3つのコマンドの挙動を以下のように統一する:

### 共通パターン

1. **リージョンなし**: カーソル行のエントリを処理し、`next-line` に委譲して次行へ移動する
2. **リージョンあり**: `TreeDiredEntryResolver.resolveRange()` で範囲内のエントリを取得し、一括処理する。処理後にリージョンをクリアする（`window.clearMark()`）。次行移動はしない

### 変更対象

- `TreeDiredMarkCommand`: リージョン対応を追加
- `TreeDiredUnmarkCommand`: リージョン対応を追加
- `TreeDiredToggleMarkCommand`: 単一行処理時の `next-line` 委譲を追加

### ADR 0083 のキーバインド表の更新

| キー | コマンド | 動作 |
|------|---------|------|
| m | tree-dired-mark | リージョンがあれば範囲内をマーク、なければカーソル行をマークし次行移動 |
| u | tree-dired-unmark | リージョンがあれば範囲内をアンマーク、なければカーソル行をアンマークし次行移動 |
| t | tree-dired-toggle-mark | リージョンがあれば範囲内をトグル、なければカーソル行をトグルし次行移動 |

## 影響

- `TreeDiredMarkCommand`, `TreeDiredUnmarkCommand`, `TreeDiredToggleMarkCommand` の3ファイルに変更が入る
- 既存の単一行動作への影響はない（mark/unmark は元から次行移動していた）
- toggle の単一行動作にのみ次行移動が追加される点が挙動変更
