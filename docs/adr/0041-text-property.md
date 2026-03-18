# ADR 0041: テキストプロパティシステム

## ステータス

承認

## コンテキスト

ミニバッファのプロンプト文字列をread-onlyにしたい。
現在はpromptLengthを覚えて確定時にスキップしているが、
バックスペースやC-aでプロンプト部分に入り込める。

Emacsではテキストプロパティ（文字範囲に任意のキーバリューを付与する仕組み）で
read-only等を実現している。将来のface付与やinvisible等にも使える汎用基盤として
テキストプロパティシステムを導入し、最初の実装としてread-onlyフラグを載せる。

## 決定

### TextProperty<T>

- 型付きプロパティキー
- `TextProperty.READ_ONLY` をBoolean型として定義

### TextPropertyStore

- 範囲（start, end）とプロパティ・値のペアを管理する
- テキスト挿入時: 挿入位置以降の範囲をシフト。rear-nonsticky（範囲末尾での挿入はread-onlyに含めない）
- テキスト削除時: 削除範囲に応じて範囲を縮小・除去
- 指定位置のプロパティ値の問い合わせ
- 内部データ構造にはEclipse Collectionsを使用

### Buffer統合

- Bufferインターフェースにプロパティ操作APIを追加
  - `putTextProperty(int start, int end, TextProperty<T> property, T value)`
  - `getTextProperty(int index, TextProperty<T> property): Optional<T>`
  - `removeTextProperty(int start, int end, TextProperty<T> property)`
- EditableBuffer: TextPropertyStoreをフィールドに持ち、insertText/deleteTextでread-onlyチェックと範囲調整
- MessageBuffer等のread-onlyバッファ: no-op実装

### read-onlyチェック

- insertText/deleteTextで操作対象の範囲にread-onlyプロパティがある場合、ReadOnlyBufferExceptionをスロー
- コマンドループでReadOnlyBufferExceptionをcatchし、メッセージバッファに表示
- undo操作のread-only連動は将来対応とする

### ミニバッファへの適用

- MinibufferInputPrompterでプロンプト文字列にread-onlyプロパティを設定
- cleanup時は削除前にread-onlyプロパティを解除

## 影響

- バッファの特定範囲にプロパティを付与する汎用基盤が整う
- ミニバッファのプロンプトが編集不可になり、操作性が向上する
- 将来的にface、invisible等のプロパティを追加可能
