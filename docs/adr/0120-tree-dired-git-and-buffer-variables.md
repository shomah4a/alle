# ADR 0120: tree-dired-git minor mode とバッファ変数によるカスタマイズ基盤

## ステータス

承認済み

## コンテキスト

tree-dired にgit関連の情報（ステータス、ブランチ名）を表示し、git操作を提供する minor mode を追加したい。

dired 固有のJava APIに依存すると、スクリプトレイヤーから同じ拡張ができないという問題がある。
スクリプトからは `TreeDiredMode` クラスが見えないため、dired のカスタマイズポイントはバッファ変数のような汎用的な仕組みを介して公開する必要がある。

また、minor mode の disable 時にクリーンアップ処理を実行するための仕組みが不足していた。
モード自身のライフサイクルコールバックと、外部からのフック機構の両方が必要である。

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

#### 設計判断: モード自身のライフサイクル vs 外部フック

- `onEnable`/`onDisable` はモード実装自身の責務（バッファ変数の設定/クリアなど）
- ModeRegistry のフックは外部（スクリプト等）がモードのライフサイクルに介入するためのカスタムポイント
- この分離により、モード実装とカスタマイズの責務が明確になる

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
TreeDiredRenderer がこの変数を読み取り、mtime と name の間にカスタムカラムを描画する。

`DiredCustomColumn` はインターフェースとして定義する:

```java
public interface DiredCustomColumn {
    String header();
    String renderCell(Path path);
    default Optional<FaceName> face(Path path) { return Optional.empty(); }
    static DiredCustomColumn of(String header, Function<Path, String> renderer) { ... }
}
```

- `renderCell` でセル文字列を生成
- `face` でセルに適用する FaceName を返す（オプショナル）
- `of` ファクトリメソッドでテキストのみの簡易カラムを生成可能

#### 設計判断: record ではなくインターフェースを選択した理由

当初は `record DiredCustomColumn(String header, Function<Path, String> renderer)` だったが、
face 対応の追加に際してインターフェースに変更した。
record にフィールドを追加するとすべてのコンストラクタ呼び出しに影響するが、
インターフェースの default メソッドなら face 不要なカラムは `renderCell` のみ実装すればよい。
`of` ファクトリメソッドで簡易な利用パターンも維持できる。

### 4. dired のルート行サフィックス

バッファ変数 `dired-root-suffix` に `String` を格納する。
TreeDiredRenderer がルートディレクトリ行の末尾にサフィックスを追加する。

### 5. dired リフレッシュフック

バッファ変数 `dired-refresh-hooks` に `ListIterable<Runnable>` を格納する。
`TreeDiredBufferUpdater.update` の先頭で各 Runnable を実行し、
レンダリング前にカスタムカラム等のデータを再取得できるようにする。

これにより、`g` キーによるリフレッシュ時に git status も再取得される。

### 6. diff 汎用 FaceName

git ステータスの色分けにあたり、既存の FaceName を流用するとセマンティクスが崩れるため、
diff/vc 汎用のセマンティック名を追加した:

- `DIFF_ADDED` — 追加された要素（git A、diff + 等）: 緑
- `DIFF_MODIFIED` — 変更された要素（git M 等）: 黄
- `DIFF_DELETED` — 削除された要素（git D、diff - 等）: 赤

git 専用ではなく、将来の diff-mode や vc-mode でも共通して使えるレベルの抽象度とした。

### 7. tree-dired-git minor mode

git コマンドの実行は `GitStatusProvider` インターフェースで外部化する。

#### 提供する機能

- **git ステータスカラム**: `DiredCustomColumn` 実装でステータス（M/A/D/?等）を表示、face で色分け
- **ブランチ名表示**: ルート行サフィックスにブランチ名を `[branch]` 形式で表示
- **`A` キー**: `tree-dired-git-add` — カーソル行またはマーク済みファイルを git add
- **`D` キー**: `tree-dired-git-delete` — git 管理下なら git rm、管理外なら通常の FileOperations.delete
- **`R` キー**: `tree-dired-git-rename` — git 管理下なら git mv、管理外なら通常の FileOperations.move

#### ライフサイクル

- `onEnable`: バッファ変数にカスタムカラム・サフィックス・リフレッシュフックを設定
- `onDisable`: バッファ変数をクリア

#### 自動有効化

`addMajorModeHook("tree-dired", ...)` で `.git` ディレクトリの存在を検出し自動有効化する。
TreeDiredCommand に git の知識を漏らさない。

#### D/R キーの設計判断: minor mode キーマップによるオーバーライド

minor mode のキーマップは major mode のキーマップより優先されるため、
tree-dired-git が有効な場合は D/R が git 版コマンドに差し替わる。
git 管理下のファイルは `git rm`/`git mv` で操作し、
管理外のファイルは `FileOperations` にフォールバックする。
`git ls-files --error-unmatch` でファイルごとに管理下かどうかを判定する。

#### GitStatusProvider インターフェース

```java
public interface GitStatusProvider {
    MapIterable<Path, String> getFileStatuses(Path rootDirectory);
    String getBranch(Path rootDirectory);
    void stageFiles(Path rootDirectory, ListIterable<Path> files);
    boolean isTracked(Path rootDirectory, Path file);
    void removeFiles(Path rootDirectory, ListIterable<Path> files, boolean force);
    void moveFile(Path rootDirectory, Path source, Path destination);
}
```

## 根拠

### バッファ変数を選択した理由

- スクリプトレイヤーからも同じ仕組みで拡張可能
- dired 固有の Java API への依存を避け、汎用的なカスタマイズ基盤を提供
- Emacs の buffer-local variable と同様のモデルで、実績のあるアプローチ
- 型安全性の喪失はトレードオフだが、カスタマイズの柔軟性を優先

### モードライフサイクルを2層に分けた理由

- モード自身のライフサイクル（`onEnable`/`onDisable`）と外部フックは責務が異なる
- モード実装者は `onEnable`/`onDisable` でセットアップ/クリーンアップを書く
- 外部（スクリプト）は ModeRegistry のフックでモードのイベントに介入する
- ModeRegistry の disable フックは既存の enable フックと対称的で、一貫性が保たれる

### diff 汎用 FaceName を追加した理由

- 既存の FaceName（WARNING, STRING 等）を色の類似で流用するとセマンティクスが崩れる
- FaceName は「見た目ではなく意味上の役割」を表す設計
- 「追加」「変更」「削除」は diff/vc で共通する概念であり、git 専用にする必要はない

## 影響

- Buffer インターフェースに 3 メソッド追加（TextBuffer, MessageBuffer で実装）
- MajorMode / MinorMode インターフェースに onEnable / onDisable デフォルトメソッド追加
- ModeRegistry に 4 メソッド追加（disable フック）
- ModeCommand にライフサイクルコールバックと disable フック呼び出し追加
- TreeDiredRenderer にカスタムカラム・サフィックス・カスタムカラム face 対応追加
- TreeDiredBufferUpdater にリフレッシュフック機構追加、クラスとメソッドを public 化
- TreeDiredEntryResolver のクラスと一部メソッドを public 化（git サブパッケージからのアクセス用）
- TreeDiredCommand に ModeRegistry 追加（majorModeHook 発火のため）
- FaceName に DIFF_ADDED / DIFF_MODIFIED / DIFF_DELETED 追加
- DefaultFaceTheme に diff 用の色定義追加
- DiredCustomColumn を record からインターフェースに変更
