# ADR-0015: LanternaによるTUI描画層

## ステータス

承認済み

## コンテキスト

alle-coreにバッファ、ウィンドウ、コマンドシステム、基本編集コマンドが揃った。
実際にターミナルで動作するエディタにするため、TUI描画層が必要である。

## 決定

### TUIライブラリ

Lanternaを採用する。全画面ターミナル制御に適しており、Java向けTUIライブラリとして実績がある。

### モジュール構成

- `alle-tui` — TUI固有のコード（Lanternaとの統合、画面描画、キー入力変換）。`alle-core`に依存
- `alle-app` — アプリケーションエントリポイント。`alle-core`と`alle-tui`に依存。将来GUI等の別フロントエンドに切り替える場合を考慮し、Mainクラスはここに配置する

### 描画とCommandLoopの統合

`CommandLoop.run()`は使わず、`processKey()`を利用してTUI側で独自ループを制御する。
「入力→処理→描画」のサイクルをTUI側が管理することで、alle-coreへの変更を不要にする。

### キー入力変換

LanternaのKeyStrokeとalleのKeyStrokeは名前が同じだが別の型である。
`KeyStrokeConverter`クラスでLanterna→alleの変換を行い、型の衝突を局所化する。
特殊キー（矢印キー等）のサポートは初回スコープ外とし、文字入力と修飾キーのみ変換する。

### 終了処理

- `TerminalInputSource`にshutdownフラグを持たせ、終了コマンドがフラグを立てる
- 次の`readKeyStroke()`呼び出しでemptyを返し、ループが自然に終了する
- Main側でtry-finallyにより`Screen.stopScreen()`と`Terminal.close()`を確実に呼ぶ

### 初回スコープ

- アクティブウィンドウのバッファ内容の描画
- カーソル位置の反映
- 文字入力（ASCII印字可能文字）
- C-qによる終了
- ステータスライン、ミニバッファ描画は後続タスク

## 帰結

- ターミナルで起動し、文字入力とカーソル移動ができるエディタが動作する
- alle-coreへの変更なしにTUI層を追加できる
- Lanterna依存はalle-tuiモジュールに閉じ込められる
