# ADR 0123: ステータスライン（モードライン）カスタマイズ機構

## ステータス

承認

## コンテキスト

現在のモードライン生成は `RenderSnapshotFactory.buildModeLineText()` にハードコードされており、
モードごとの表示変更や汎用スロット（git-status等）の追加が困難である。
Emacsのモードラインのように宣言的にフォーマットを定義し、部分的に差し替え可能な仕組みが必要。

## 決定

### スロットベースの宣言的フォーマット定義を導入する

#### コア概念

- **StatusLineElement**: スロットとグループの共通インターフェース。`render(StatusLineContext) → String`
  - **StatusLineSlot**: 末端の単一ハンドラ（関数型インターフェース）
  - **StatusLineGroup**: 子StatusLineElementのリストを持つCompositeノード
- **StatusLineFormatEntry**: フォーマット定義の要素。リテラル文字列とスロット名参照を型レベルで区別する
- **StatusLineRegistry**: 名前 → StatusLineElement のフラットなマッピング（グローバルに1つ）
- **StatusLineRenderer**: フォーマット定義を走査し、レジストリからハンドラを引いて連結する
- **StatusLineContext**: Window, BufferFacade の読み取り専用record

#### フォーマット定義

- `Setting<ListIterable<StatusLineFormatEntry>>` として型安全に定義
- `BufferLocalSettings` 経由でバッファローカルに差し替え可能
- 5層の優先順位解決（バッファローカル → マイナーモード → メジャーモード → グローバル → デフォルト）の恩恵を受ける

#### スロットグループ

フォーマット定義にスロット名を直接列挙するのではなく「グループ」を参照する構造とする。
汎用スロット（git-status等）をグループに追加すれば、全モードのフォーマットに自動的に波及する。

標準グループ:
- buffer-info: buffer-status, truncate-indicator, buffer-name
- position: line-number, column-number
- mode-info: major-mode, minor-modes
- misc-info: git-status

#### git-statusスロット

- GitBranchProviderインターフェースを経由して外部プロセス呼び出しを抽象化
- CachingGitBranchProviderデコレータでCaffeine による時間ベースキャッシュ（TTL 5秒）を提供
- キャッシュキーはgitリポジトリルートのPath単位

#### RenderSnapshotFactoryへの統合

- `create()` にStatusLineRendererを受け取るオーバーロードを追加
- 旧シグネチャはデフォルトRendererで委譲し、既存テストの一斉修正を回避

#### 未登録スロット名の扱い

- 描画パス中に例外をスローしてはならない
- 未登録名は空文字列として扱い、*Warnings* バッファにログ出力する

### 検討した代替案

- **CommandContextをハンドラの入力にする**: コマンド実行に特化した構造であり、描画用途には過剰。書き込み能力（delegate等）まで渡ってしまう
- **スロットレジストリに優先度階層を導入する**: フォーマット定義がバッファローカルで差し替え可能な時点で、モードごとの表示カスタマイズは吸収できる。スロット登録自体はフラットで十分
- **バッファ変数（getVariable）でフォーマット定義を保持する**: Object型でキャスト不安。Setting<T> + BufferLocalSettingsの方が型安全

## 結果

- 新規パッケージ `core.statusline` の追加
- Caffeine依存の追加
- `RenderSnapshotFactory` のモードライン生成がStatusLineRenderer経由に変更
- `EditorCore` にStatusLineRegistry初期化が追加
