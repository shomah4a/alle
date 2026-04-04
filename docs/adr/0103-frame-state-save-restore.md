# ADR 0103: フレーム状態の保存・復元

## ステータス

承認

## コンテキスト

ウィンドウの分割状態・表示中のバッファ・カーソル位置といったフレームのレイアウト情報は、ウィンドウ操作で容易に失われる。
作業コンテキストを名前付きで保存し、任意のタイミングで復元できる機能が必要である。
ADR 0101/0102 で導入した BufferHistoryEntry (BufferIdentifier + ViewState) の仕組みを流用する。

## 決定

### スナップショットのデータモデル

#### WindowSnapshot

ウィンドウ1つ分の状態を表す。現在バッファと履歴を型レベルで分離する。

```java
public record WindowSnapshot(
    BufferHistoryEntry current,
    ImmutableList<BufferHistoryEntry> history,
    boolean truncateLines
) {}
```

#### WindowTreeSnapshot

WindowTree の構造をミラーする sealed interface。

```java
public sealed interface WindowTreeSnapshot {
    record Leaf(WindowSnapshot snapshot) implements WindowTreeSnapshot {}
    record Split(Direction direction, double ratio,
                 WindowTreeSnapshot first, WindowTreeSnapshot second) implements WindowTreeSnapshot {}
}
```

#### FrameSnapshot

フレーム全体のスナップショット。activeWindowIndex は WindowTree.windows() の深さ優先順インデックス。

```java
public record FrameSnapshot(WindowTreeSnapshot tree, int activeWindowIndex) {}
```

### Window の復元

setBuffer() はバッファ切り替え時に履歴追加の副作用を持つため、復元用途ではこの経路を使わない。
パッケージプライベートの static ファクトリメソッドで、バッファ・履歴・ViewState を直接設定した Window を生成する。

### バッファ不在時のフォールバック

復元時にバッファが存在しない場合の振る舞い:
1. current のバッファが不在 → history から生存するバッファを順に探索
2. history にも生存バッファがない → `*scratch*` バッファにフォールバック
3. ウィンドウ分割構造は常に復元する（バッファ不在でもツリーは縮退させない）

### 名前付き保存の管理

FrameLayoutStore クラスをフレームの外側に配置する。
フレームをまたいでの状態復元を可能にするため、Frame 自体には保存機能を持たせない。

```java
public class FrameLayoutStore {
    void save(String name, FrameSnapshot snapshot);
    Optional<FrameSnapshot> load(String name);
    ImmutableSet<String> names();
}
```

### コマンド

- `save-frame-state`: ミニバッファで名前を入力し、現在のフレーム状態を保存する
- `restore-frame-state`: ミニバッファで名前を入力（補完あり）し、フレーム状態を復元する

### 補完

FrameLayoutNameCompleter を作成し、restore-frame-state コマンドで保存済みレイアウト名を補完する。

## 結果

- ウィンドウ分割状態・バッファ・カーソル位置を名前付きで保存・復元できる
- バッファ不在時は履歴からフォールバックし、ウィンドウ分割構造は維持される
- FrameLayoutStore がフレームの外側にあるため、将来的にフレーム間での状態復元が可能
