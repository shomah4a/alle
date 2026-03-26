# ADR 0063: MinibufferInputPrompter の core 層移動

## ステータス

承認

## コンテキスト

`MinibufferInputPrompter` は `alle-tui` パッケージに配置されていたが、
Lanterna 等の TUI ライブラリに一切依存しておらず、全依存が core 層のクラスのみであった。
tui 層は設計上 core から切り離せるようになっているべき層であり、
core のクラスのみに依存するロジックが tui 層にあることは層の独立性を損なう。

内部クラスとして定義されている5つのミニバッファコマンド
（Confirm, Cancel, Complete, PreviousHistory, NextHistory）およびキーマップ構築ロジックも、
すべて core 層の `Frame`, `Window`, `Buffer` 等を操作しているだけであり、
tui 固有の処理は含まれていない。

## 決定

### MinibufferInputPrompter を core.input パッケージに移動

- パッケージ: `io.github.shomah4a.alle.tui` → `io.github.shomah4a.alle.core.input`
- 内部クラス（コマンド群）はそのまま内部クラスとして維持
- ロジックの変更は行わない（パッケージ変更のみ）

### テストクラスも core 側に移動

- `MinibufferInputPrompterTest` を `alle-core/src/test/java` に移動

## 影響

- `InputPrompter` インターフェースと同じパッケージに実装クラスが配置され、凝集度が向上する
- tui 層から core ロジックの漏出が解消される
- `Main.java` の import 変更のみで、ファクトリ引数の型変更は不要
