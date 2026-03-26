# ADR 0062: QuitCommand の core 層移動

## ステータス

承認

## コンテキスト

`QuitCommand` は `alle-tui` パッケージに配置されており、`TerminalInputSource`（Lanterna 固有）に直接依存していた。
tui 層は設計上 core から切り離せるようになっているべき層であり、ここにエディタ終了というコアロジックが存在すると、
層の独立性が損なわれる。

core には既に `ShutdownRequestable` インターフェースがあり、`SaveBuffersKillAlleCommand` がそれを利用する先行パターンとして存在する。
`EditorCore.create` も `ShutdownRequestable` を引数で受け取っており、core 内で完結可能な状態にある。

また、既存の `KeyboardQuitCommand`（C-g、操作キャンセル）と `QuitCommand`（プロセス終了）は名前が紛らわしいため、
リネームにより意図を明確化する。

- ADR 0037 で「`QuitCommand` は alle-tui に残存」と決定していたが、本 ADR で変更する
- ADR 0040 で「TUI 固有のコマンド（`QuitCommand`）」として扱っていたが、本 ADR で変更する

## 決定

### QuitCommand → ProcessQuitCommand へリネームし core に移動

- クラス名: `ProcessQuitCommand`
- パッケージ: `io.github.shomah4a.alle.core.command`
- コンストラクタ引数: `ShutdownRequestable`（`TerminalInputSource` ではなく）
- コマンド名: `quit`（変更なし）

### EditorCore 内で登録・バインド

- `ProcessQuitCommand` の登録と C-q バインドを `EditorCore.create` 内で行う
- `Main.java` からは QuitCommand 関連のコードを削除する

### alle-tui から QuitCommand を削除

- `alle-tui/src/main/java/.../QuitCommand.java` を削除する

## 影響

- `SaveBuffersKillAlleCommand` と同一のパターン（`ShutdownRequestable` 経由）に統一される
- tui 層からコアロジックの漏出が解消される
- `Main.java` の TUI 固有コマンド追加コードが不要になる
- `KeyboardQuitCommand`（操作キャンセル）と `ProcessQuitCommand`（プロセス終了）で名前の混同がなくなる
