# ADR-0017: コマンドレジストリ・設定変数レジストリ・コマンドコンテキスト整備

## ステータス

承認済み

## コンテキスト

現在のコマンドは個別のJavaクラスとして実装され、Main.javaでインスタンス生成→Keymapに直接バインドしている。コマンドを名前で検索・実行する手段がなく、将来のスクリプト評価基盤（GraalVM Polyglot API）からの呼び出しや、Emacsの`M-x`相当の機能が実現できない。

また、`truncate-lines`のようなバッファごとに異なる値を持つ設定変数の仕組みや、C-p/C-nの`temporary-goal-column`のようなコマンド間で引き継ぐ状態の管理機構も存在しない。

## 決定

### Phase 1: コマンドレジストリ

`CommandRegistry`クラスを新設する。

- `register(Command)` — `command.name()`をキーに登録。同名二重登録は`IllegalStateException`
- `lookup(String name)` → `Optional<Command>`
- `registeredNames()` → イミュータブルコレクション

コマンドインスタンスはレジストリに登録したものをKeymapバインドにも使い、同一性を保証する。

### Phase 2: 設定変数レジストリ

Emacsの`defvar`/`defcustom`とバッファローカル変数に相当する仕組みを導入する。

- `Setting<T>` record — key, type(Class<T>), defaultValueを保持
- `SettingsRegistry` — Setting定義の登録とグローバルデフォルト値管理
- `BufferLocalSettings` — バッファごとのローカル値管理

値の取得は`Class.cast()`による実行時型チェックで型安全性を確保する（`unchecked cast`を回避）。デフォルト値→バッファローカル値の2層で解決する。デフォルト値は常に非null。

### Phase 3: コマンドコンテキスト拡張

Emacsの`this-command`/`last-command`に相当する仕組みを導入する。

- `CommandContext`に`lastCommand`/`thisCommand`（`Optional<String>`）を追加
- `CommandLoop`がコマンド実行前後に状態を管理
- `Window`に`temporaryGoalColumn`を追加（C-p/C-n実装時に使用）

### Phase 4: コマンド名による実行

- `ExecuteCommandCommand`を新設し、レジストリからコマンド名で引いて実行する
- Emacsの`M-x`に相当する基盤（ミニバッファUIは別途）

### スクリプト評価基盤との関係

将来GraalVM Polyglot APIでスクリプト層を導入する際、`CommandRegistry`と`SettingsRegistry`がスクリプトからのAPI面となる。現段階ではJava内部APIとして実装し、公開時にイミュータブルなビューラッパーを追加する方針とする。

## 帰結

- コマンドが名前で検索・実行可能になり、スクリプトからのコマンド呼び出しの基盤が整う
- バッファローカル設定により`truncate-lines`等の設定がバッファ単位で管理できる
- コマンド間の状態引き継ぎにより、C-p/C-n連続実行時のgoal-column保持等が実現できる
- `unchecked cast`禁止制約下で型安全な設定値管理を`Class.cast()`方式で実現する
