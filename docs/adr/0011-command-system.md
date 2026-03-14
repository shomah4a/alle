# ADR-0011: コマンドシステム

## ステータス

承認済み

## コンテキスト

エディタのController層として、ユーザー入力を操作に変換するコマンドシステムが必要である。
IO/レンダリング（View層）の前に、コマンドシステムのアーキテクチャを固めておくことで、
後のView層実装がスムーズになる。

## 決定

### Command

- `Command`は名前付きの操作単位を表すインターフェースとする
- `execute(CommandContext)`メソッドで実行する
- 戻り値は現時点では`void`とする
  - undo/redo導入時に戻り値の追加を検討する（将来の拡張ポイント）

```java
public interface Command {
    String name();
    void execute(CommandContext context);
}
```

### CommandContext

- コマンド実行時のコンテキスト情報を保持するレコード
- `Frame`と`BufferManager`を保持する
- C-uプレフィックス引数は後付けで追加可能（フィールド追加のみで既存Commandに影響なし）

```java
public record CommandContext(Frame frame, BufferManager bufferManager) {}
```

### バッファアクセスパスの方針

- 編集操作: `context.frame().getActiveWindow()` 経由でバッファにアクセスする
- バッファの作成・削除・一覧取得: `context.bufferManager()` 経由
- この二重経路は意図的なものであり、操作の性質に応じて使い分ける

### KeySequence

- 修飾キー（Ctrl, Alt/Meta, Shift）とキーの組み合わせを表現する
- `C-x C-s`のような複合キーシーケンスにも対応する

### Keymap

- `KeySequence`から`Command`または子`Keymap`（プレフィックスキー用）への対応付けを持つ
- Keymap自体は純粋なマッピングのみを担当する
- 複数Keymapの優先順位付きルックアップは`KeyResolver`等の別クラスに委譲する
  - これによりMode導入時の影響範囲を限定できる

### キーマップ優先順位（将来のMode導入時）

マイナーモード → メジャーモード → グローバルの順で探索する。
この優先順位ロジックはKeymapではなくKeyResolverが管理する。

## パッケージ構成

- `io.github.shomah4a.alle.core.command` — Command, CommandContext
- `io.github.shomah4a.alle.core.keybind` — KeySequence, Keymap, KeyResolver

commandとkeybindが分離されていることで、コマンドはキーバインドなしでも呼び出せる（M-x相当）。

## 帰結

- 既存コード（Buffer, Window, Frame等）への変更は不要で、新規追加のみ
- コマンドはキーバインドから独立しており、テストやスクリプトから直接呼び出せる
- Keymapと優先順位ロジックが分離されているため、Mode導入時にKeymapの変更が不要
- C-uプレフィックス引数はCommandContextへのフィールド追加で後付け可能
