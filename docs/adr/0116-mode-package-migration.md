# ADR 0116: モード実装クラスの modes サブパッケージへの移動

## ステータス

承認済み

## コンテキスト

`io.github.shomah4a.alle.core.mode` パッケージにはモードシステムのインフラ（`MajorMode`, `MinorMode`, `ModeRegistry`, `AutoModeMap`）と具体的なモード実装（`TextMode`, `JavaScriptMode`, `JsonMode`, `MarkdownMode`, `YamlMode`）が混在している。

一方、`modes/dired/` や `modes/occur/` のように、モード固有の実装は `modes` サブパッケージに配置する方針が既に確立されている。

## 決定

具体的なモード実装クラスを `modes` 以下のサブパッケージに移動する。

| クラス | 移動先パッケージ |
|---|---|
| `TextMode` | `mode.modes.text` |
| `JavaScriptMode` | `mode.modes.javascript` |
| `JsonMode` | `mode.modes.json` |
| `MarkdownMode`, `MarkdownStyler` | `mode.modes.markdown` |
| `YamlMode` | `mode.modes.yaml` |

`mode/indent/` パッケージは複数モードから共有されるユーティリティであるため、現在の位置に残す。

## 理由

- モードインフラと具体的なモード実装の責務を明確に分離する
- 既存の `modes/dired/`, `modes/occur/` と一貫した構造にする
