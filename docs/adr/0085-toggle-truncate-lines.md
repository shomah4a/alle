# ADR 0085: 行折り返しモード（toggle-truncate-lines）

## ステータス

承認

## コンテキスト

現在のエディタは行の切り詰め（truncation）のみをサポートしており、
ウィンドウ幅を超える行は `displayStartColumn` による水平スクロールで表示している。
Emacsの `toggle-truncate-lines` に相当する行折り返しモードが必要になった。

基本は折り返しモード（wrap）とし、トグルで切り替え可能にする。

## 決定

### 折り返しの単位

Window単位で `truncateLines` フラグを持つ。
同一バッファを複数ウィンドウで表示した場合、各ウィンドウが独立した折り返し設定を持てる。

### LineSnapshotの設計

折り返しによって1バッファ行が複数の視覚行に展開される。
LineSnapshotには以下の情報を追加する:

- 元バッファ行のテキスト全体とスパンはそのまま保持
- 視覚行の開始/終了コードポイントオフセットを追加フィールドとして持つ

ScreenRendererが視覚行の描画範囲を制限する方式とし、
既存のスパン・リージョン処理を流用する。

### 全角文字の折り返し

表示幅の末尾に全角文字（2カラム幅）が収まらない場合、
全角文字は次の視覚行に送り、末尾にはパディング（空白）を入れる。

### 視覚行計算の共通化

折り返し時の視覚行計算は以下の複数箇所で必要となる:

- RenderSnapshotFactory（視覚行展開、ensurePointVisible、カーソル位置計算）
- ScrollUpCommand / ScrollDownCommand
- NextLineCommand / PreviousLineCommand

これらで共通利用する `VisualLineUtil` ユーティリティクラスを新規作成する。

### 水平スクロールとの排他

折り返しモード時は水平スクロールを無効化する（`displayStartColumn = 0` 固定）。
切り詰めモード時は従来通り水平スクロールが有効。

### 段階的実装

実装中の安全性を確保するため、`truncateLines` のデフォルト値は `true`（現在の動作=切り詰め）で開始する。
全機能が実装・テスト完了した後に、デフォルトを `false`（折り返し）に変更する。

## 影響

### 変更対象

- Window: `truncateLines` フラグ追加
- RenderSnapshotFactory: 視覚行展開ロジック追加
- RenderSnapshot: LineSnapshotに視覚行範囲情報追加
- ScreenRenderer: 視覚行範囲に基づく描画制限
- EditorCore: コマンド登録
- ScrollUpCommand / ScrollDownCommand: 視覚行単位スクロール
- NextLineCommand / PreviousLineCommand: 視覚行単位移動

### 新規ファイル

- VisualLineUtil: 視覚行計算ユーティリティ
- ToggleTruncateLinesCommand: トグルコマンド
