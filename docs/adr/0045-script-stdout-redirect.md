# ADR-0045: スクリプトの stdout/stderr をエディタバッファにリダイレクト

## ステータス

提案中

## コンテキスト

GraalPy スクリプトエンジン（ADR-0044）の導入により、Python スクリプトの `print()` や例外出力が
ターミナルの標準出力に直接流れてしまい、エディタ画面と干渉する。
スクリプトの出力をエディタ内で確認できるようにし、開発体験を向上させたい。

## 決定

### Python の stdout/stderr を専用 MessageBuffer にリダイレクトする

- `*Python Output*` バッファ: `sys.stdout` の出力先
- `*Python Error*` バッファ: `sys.stderr` の出力先

### MessageBufferOutputStream を導入する

`java.io.OutputStream` を実装し、書き込まれたバイト列を UTF-8 デコードして
改行ごとに `MessageBuffer.message()` で1行ずつ追加するアダプタを作成する。

### GraalVM Context.Builder の .out() / .err() で設定する

`Context.newBuilder("python").out(stdoutStream).err(stderrStream)` により、
GraalPy の標準出力・標準エラー出力を OutputStream に接続する。

### GraalPyEngineFactory には OutputStream を渡す

ファクトリは `MessageBuffer` を直接知らず、`OutputStream` のみを受け取る。
`MessageBufferOutputStream` の生成はアプリケーション層（Main.java）で行う。
これによりファクトリの責務を「GraalPy Context の生成」に限定する。

## 影響

- `alle-script` に `MessageBufferOutputStream` を追加
- `GraalPyEngineFactory` のコンストラクタに `OutputStream` 2つを追加
- `Main.java` でバッファ生成と BufferManager への登録を追加

## 設計上の注意点

- `flush()` で未送出のバッファ残をすべて送出すること
- UTF-8 マルチバイトシーケンスのバイト境界分割に対応すること
- スレッド安全性は現在の同一スレッド実行の前提に依存する
