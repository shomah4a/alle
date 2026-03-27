# ADR-0068: revert-buffer コマンド

## ステータス

承認済み

## コンテキスト

バッファの内容をファイルから再読み込みする revert-buffer コマンドが必要である。
Emacsの revert-buffer に相当する機能で、ファイルの外部変更を取り込む場面や、
編集内容を破棄してファイルの状態に戻したい場面で使用する。

## 決定

### コマンド仕様

- コマンド名: `revert-buffer`
- アクティブバッファにファイルパスが設定されていない場合はメッセージを表示して中断する
- readOnlyバッファの場合はメッセージを表示して中断する
- バッファがdirtyな場合、ミニバッファで "yes" / "no" の確認を行う
  - 補完機能（Tab）で "yes", "no" を選択可能にする
  - "yes" の場合のみ revert を実行する
- revert後はundo/redo履歴をクリアする
- revert後はdirtyフラグをクリアする
- カーソル位置はバッファ長に収まるよう補正する

### 実装方針

- `UndoManager` に `clear()` メソッドを追加する
- `BufferIO` に `reload()` メソッドを追加する
  - ファイル読み込みはロック外で行い、バッファ操作は `atomicOperation` + `withoutRecording` で保護する
- `RevertBufferCommand` クラスを新規作成する
  - yes/no確認のCompleterはstaticフィールドとして定義する

## 影響

- `UndoManager`: `clear()` メソッド追加（新規のみ、既存影響なし）
- `BufferIO`: `reload()` メソッド追加（新規のみ、既存影響なし）
- `EditorCore`: コマンド登録行の追加
