# ADR-0013: CommandLoopと入力抽象化

## ステータス

承認済み

## コンテキスト

キー入力→コマンド解決→コマンド実行のメインループが必要である。
また、通常の文字入力をコマンドとして扱う仕組み（self-insert-command）も必要である。

## 決定

### CommandContextにトリガーKeyStrokeを追加

- コマンドが「何のキーで呼ばれたか」を参照できるようにする
- `Optional<KeyStroke>`で保持（M-x等プログラム的呼び出し時はempty）

```java
public record CommandContext(
    Frame frame,
    BufferManager bufferManager,
    Optional<KeyStroke> triggeringKey
) {}
```

### InputSource

- キー入力の抽象化インターフェース
- `Optional<KeyStroke>`を返し、emptyで入力終了を表現
- テスト時に有限個のキー入力を返すスタブが容易に作れる

```java
public interface InputSource {
    Optional<KeyStroke> readKeyStroke();
}
```

### SelfInsertCommand

- triggeringKeyのkeyCodeに対応する文字をカーソル位置に挿入する
- keyCodeの有効性チェック（Character.isValidCodePoint）を行う
- 修飾キー付きの場合は挿入しない

### CommandLoop

- InputSource → KeyResolver → Command実行のメインループ
- InputSourceがemptyを返すとループ終了
- 1ステップを独立メソッドとして抽出し、テスタビリティを確保
- プレフィックスキー（C-x C-s等）に対応：PrefixBindingの場合は次のキー入力を待ち、子Keymapで解決
- キーマップに未マッチのキーは無視される（暗黙のself-insertは行わない）
- self-insert-commandは明示的にキーマップでバインドする（Keymap.bindPrintableAscii()で一括バインド可能）

## パッケージ構成

- `io.github.shomah4a.alle.core.command` — CommandContext, SelfInsertCommand, CommandLoop
- `io.github.shomah4a.alle.core.input` — InputSource

## 帰結

- エディタのメインループが構築され、キー入力からコマンド実行までのパイプラインが繋がる
- 通常の文字入力もコマンドとして統一的に扱える
- InputSourceの抽象化により、TUI/GUI/テスト等異なる入力ソースに対応可能
- CommandContextのrecordフィールド追加により既存テストの修正が必要
