# ADR 0120: tree-dired-git minor mode とバッファ変数によるカスタマイズ基盤

## ステータス

承認済み

## コンテキスト

tree-dired にgit関連の情報（ステータス、ブランチ名）を表示し、git add 操作を提供する minor mode を追加したい。

dired 固有のJava APIに依存すると、スクリプトレイヤーから同じ拡張ができないという問題がある。
スクリプトからは `TreeDiredMode` クラスが見えないため、dired のカスタマイズポイントはバッファ変数のような汎用的な仕組みを介して公開する必要がある。

また、minor mode の disable 時にクリーンアップ処理を実行するためのフック機構が不足している。
ModeRegistry には enable フック（`addMajorModeHook`, `addMinorModeHook`）は存在するが、disable フックがない。

## 決定

### 1. モードライフサイクルの整備

#### モードインターフェースへの onEnable / onDisable 追加

MajorMode / MinorMode インターフェースに `onEnable(BufferFacade)` / `onDisable(BufferFacade)` デフォルトメソッドを追加する。
モード実装自身のセットアップ/クリーンアップを記述する場所。

#### ModeRegistry への disable フック追加

ModeRegistry に disable フックを追加する。既存の enable フックと対称的な API。
こちらは外部（スクリプト等）がモードのライフサイクルに介入するためのカスタムポイント。

- `addMajorModeDisableHook(String modeName, BiConsumer<BufferFacade, String> hook)`
- `addMinorModeDisableHook(String modeName, BiConsumer<BufferFacade, String> hook)`
- `runMajorModeDisableHooks(String modeName, BufferFacade buffer)`
- `runMinorModeDisableHooks(String modeName, BufferFacade buffer)`

#### 呼び出し順序

ModeCommand での呼び出し順序:
- enable 時: モード切り替え → `mode.onEnable(buffer)` → `runEnableHooks`
- disable 時: `runDisableHooks` → `mode.onDisable(buffer)` → モード除去

### 2. バッファ変数ストアの追加

Buffer に Emacs の buffer-local variable に相当する untyped な key-value ストアを追加する。
既存の `BufferLocalSettings`（`Setting<T>` ベース、型安全）とは別に設ける。

```java
Optional<Object> getVariable(String key);
void setVariable(String key, Object value);
void removeVariable(String key);
```

型安全性はカスタマイズ実装側の責任とする。
バッファ変数を読み取る側（Renderer 等）は instanceof で型チェックし、不一致時はフォールバックする。

### 3. dired のカスタムカラム

バッファ変数 `dired-custom-columns` に `ListIterable<DiredCustomColumn>` を格納する。
`DiredCustomColumn` は `record DiredCustomColumn(String header, Function<Path, String> renderer)` とする。
TreeDiredRenderer がこの変数を読み取り、mtime と name の間にカスタムカラムを描画する。

### 4. dired のルート行サフィックス

バッファ変数 `dired-root-suffix` に `String` を格納する。
TreeDiredRenderer がルートディレクトリ行の末尾にサフィックスを追加する。

### 5. tree-dired-git minor mode

git コマンドの実行は `GitStatusProvider` インターフェースで外部化する。
minor mode 有効化時にバッファ変数を設定し、disable フックでクリーンアップする。

- enable 時: `dired-custom-columns` にステータスカラムを追加、`dired-root-suffix` にブランチ名を設定
- disable 時: バッファ変数をクリア

`A` キーに git add コマンドをバインドする。
`addMajorModeHook("tree-dired", ...)` で .git リポジトリ内なら自動有効化する。

## 根拠

### バッファ変数を選択した理由

- スクリプトレイヤーからも同じ仕組みで拡張可能
- dired 固有の Java API への依存を避け、汎用的なカスタマイズ基盤を提供
- Emacs の buffer-local variable と同様のモデルで、実績のあるアプローチ
- 型安全性の喪失はトレードオフだが、カスタマイズの柔軟性を優先

### disable フックを ModeRegistry に追加する理由

- 既存の enable フックと対称的で、API の一貫性が保たれる
- MajorMode / MinorMode インターフェースの変更が不要
- 外部からハンドラを追加できるため、モード実装とクリ��ンアップロジックを分離できる

## 影響

- Buffer インターフェースに 3 メソッド追加（TextBuffer, MessageBuffer で実装）
- ModeRegistry に 4 メソッド追加
- ModeCommand に disable フック呼び出し追加
- TreeDiredRenderer にカスタムカラム・サフィックス対応追加（applyFaces のオフセット計算に注意）
