# ADR 0133: 対話的シェルバッファ (M-x shell)

## ステータス

承認

## コンテキスト

既存の `shell-command` (M-!) は一回実行型であり、対話的なシェルセッションには対応していない。
Emacs の `M-x shell` に相当する、バッファ内でコマンドを繰り返し入力・実行できる対話型シェルを実装する。

## 決定

### 全体構成

OccurMode / TreeDiredMode のパターン（Mode + Model + Initializer + Command群）に準拠する。

- `ShellMode`: MajorMode 実装
- `ShellBufferModel`: バッファ状態管理（入力開始位置の追跡）
- `ShellInitializer`: コマンド・キーマップ・レジストリの組み立て
- `ShellCommand`: M-x shell エントリポイント

### プロセス抽象化

`InteractiveShellProcess` インターフェースで対話的プロセスのI/Oを抽象化する。
既存の `ShellCommandExecutor`（一回実行型、exit code を返す）とは本質的に異なるため、別インターフェースとする。

- `sendInput(String)`: stdin への書き込み
- `sendSignal(int)`: シグナル送信（SIGINT, SIGTSTP等）
- `destroy()`: プロセス終了

`DefaultInteractiveShellProcess` はファクトリメソッド `start(Path, Consumer<String>, Runnable)` で
起動済みインスタンスを返す。コンストラクタ時点でプロセスは起動済みとなり、フィールドは non-null が保証される。

`SHELL` 環境変数からユーザーのデフォルトシェルを参照する（未設定の場合は `/bin/sh`）。
`script -q /dev/null -c "stty -echo; <shell> -i"` で��似 PTY ���確保し、
シェルを対話モードで起動する。`stty -echo` でエコーバックを無効化し、
エディタ側の self-insert でユーザー入力を表示する。
`redirectErrorStream(true)` で stdout/stderr をマージし、出力順序の複雑さを回避する。
`TERM=xterm-256color` を設定し、色情報を受け取れるようにする。

出力の読み取りは `read(char[], ...)` でチャンク単位に行い、`\n` で行を分割する。
改行で終わらないフラグメント（プロンプト等）もまとまった状態でコールバックに通知する。

### ANSI SGR の扱い

ターミナル出力の色付けに対応するため、ANSI SGR エスケープシーケンスをパースして Face に変換する。
SGR 以外のエスケープシーケンス（カーソル移動・画面クリア等）は除去する。

#### SgrAttributes（shell パッケージ内、package-private）

SGR 属性（前景色・背景色・bold・underline・reverse）を保持するイミュータブルクラス。
`toFaceName()` で `"ansi-sgr:fg=red:bold"` のような規約文字列の FaceName を生成する。

このクラスはシェルモードパッケージ内に閉じ込め、他のモードからは使用できないようにする。
汎用的なスタイリング層の柔軟性を不必要に増やさないための制約。

#### DefaultFaceTheme のフォールバック

`DefaultFaceTheme.resolve()` に `"ansi-sgr:"` プレフィックスのフォールバック処理を追加する。
シェルパッケージへの依存はなく、文字列規約のデコードのみで FaceSpec を構築する。
色名は既存の FaceResolver の色名（"red", "green", "red_bright" 等）をそのまま使用する。

### バッファ名

呼び出すたびに新しいシェルバッファを作成する。
バッファ名は `*shell*`, `*shell*<2>`, `*shell*<3>`, ... と連番が付く。

### バッファの入出力管理

`ShellBufferModel` が `inputStartPosition` で入力開始位置を追跡する。

- `[0, inputStartPosition)`: 出力領域。`putReadOnly` でテキストプロパティレベルのread-only保護
- `[inputStartPosition, length)`: ユーザー入力領域。自由に編集可能

出力到着時はユーザー入力を退避→出力挿入→ユーザー入力復元の手順で処理する。
`atomicOperation` でスレッド安全性を担保する。

### キーバインド

| キー | コマンド | 動作 |
|------|----------|------|
| RET | shell-send-input | 入力をプロセス stdin へ送信 |
| C-c C-c | shell-interrupt | SIGINT 送信 |
| C-c C-z | shell-suspend | SIGTSTP 送信 |

self-insert 等はグローバルキーマップにフォールスルーする（defaultCommand は設定しない）。

### プロセスライフサイクル

- 開始: `ShellCommand` でバッファ作成時
- 終了: `ShellMode.onDisable()` → `process.destroy()`
- `BufferKiller.kill()` に `MajorMode.onDisable()` 呼び出しを追加してリソースリークを防止する
- プロセス終了検出: リーダースレッドが EOF を検出 → 終了メッセージをバッファに追記

### プロセス終了検出

`DefaultInteractiveShellProcess.start()` はプロセス終了コールバック（`Runnable onProcessExit`）を受け取る。
リーダースレッドがEOFを検出した後にコールバックを呼び出し、`ShellBufferModel.markProcessFinished()` を実行する。

### BufferKiller の修正

現状 `BufferKiller.kill()` は `MajorMode.onDisable()` を呼び出していない。
シェルバッファの場合、プロセスリソースがリークする。
既存の MajorMode（OccurMode, TreeDiredMode 等）は空の onDisable を持つため、
呼び出し追加による既存機能への副作用はない。

## 却下した代替案

### ShellCommandExecutor の拡張

一回実行型と対話型は本質的に異なるため、同一インターフェースに統合すると両方の責務が複雑化する。

### PTY 使用

Java 標準 API では PTY をサポートしていない。pty4j 等の JNI ライブラリの導入は現時点では過剰。

### FaceName への ANSI 色定数の静的追加

16色 × bold/underline の組み合わせを静的に定義するのは管理が煩雑。
SgrAttributes による動的 FaceName 生成 + DefaultFaceTheme のフォールバック解決の方が拡張性が高い。

### SgrAttributes の汎用スタイリング層への配置

ANSI SGR の透過的な扱いは便利だが、セマンティック FaceName の設計原則を崩す恐れがある。
シェルモード内に閉じ込めることで、他のモードが意図せず使用することを防ぐ。
