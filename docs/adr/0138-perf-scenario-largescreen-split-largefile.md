# ADR 0138: パフォーマンス計測シナリオの拡張（大画面+画面分割+大ファイル）

## ステータス

承認

## コンテキスト

ADR 0130 で構築した async-profiler ベースのプロファイリング基盤
（`bench/` 配下）は、80x24 のターミナルで 10,000行（約850KB）の
テストファイルに対し、単一ウィンドウでのカーソル移動・編集を中心に
計測する構成となっている。

エディタは画面分割（`split-window-below` / `split-window-right`、
`other-window`、`delete-window` 等）と複数バッファのレンダリングを
サポートしているが、現行ベンチマークではこれらの経路が踏まれていない。
また、ファイルサイズも 850KB と中規模に留まっており、ギャップバッファや
行インデックスの大規模ケースの挙動が観測しづらい。

ユーザーの実利用パターン（ターミナルを広めに開く、複数バッファを
分割して並べる、長いログ/ソースを上下にスクロールする）と現行
ベンチマークの差分が大きいため、シナリオを以下の方向で拡張する必要が
ある。

- 大きめのターミナルサイズ（例: 200x60 程度）
- 画面分割の組み合わせ（縦分割・横分割・other-window 切替・解除）
- 大きめのファイル（10万行/約8MB 程度）でのスクロール

## 方針

### ターミナルサイズの可変化と環境変数体系

`bench/driver.py` が `TIOCSWINSZ` で固定値 (24,80) を渡している箇所を、
環境変数で上書きできるようにする。未指定時のデフォルトは現行値を
維持する（既存挙動の互換性確保）。

ベンチ関連の挙動制御パラメータは `BENCH_` プレフィックスで統一する。

| 環境変数 | 既定値 | 用途 |
|---|---|---|
| `BENCH_COLS` | 80 | ターミナル列数 |
| `BENCH_ROWS` | 24 | ターミナル行数 |
| `BENCH_INIT_WAIT` | 60 | 初期化待ちの最大秒数 |
| `BENCH_INIT_MIN_BYTES` | 1000 | 初期化完了とみなす最小出力バイト数 |
| `BENCH_INIT_STABLE_SEC` | 1.0 | 出力増分が無くなって安定したとみなす待機秒数 |
| `BENCH_SLEEP_PER_CHUNK` | 0.005 | チャンク送信間隔（秒） |
| `BENCH_DURATION_MARGIN` | 15 | profile_duration に加算するマージン秒 |

初期化完了判定は従来の「累積バイト数閾値のみ」から、
**「最小バイト数を超え、かつ最後の出力から `BENCH_INIT_STABLE_SEC` 秒
増分が無い」** 状態を完了とみなす安定判定方式に変更する。
これにより大ファイル読み込み中の初期描画バーストを完了と誤検知する
リスクを軽減する。

リサイズシナリオ（実行中に `SIGWINCH` を伴うウィンドウサイズ変更）は
本ADRの対象外とする。`TIOCSWINSZ` は子プロセス起動前に1度だけ設定する。

### シナリオモードの導入

`bench/generate-keystrokes.py` を以下の3モードに拡張する。

| モード | 用途 |
|---|---|
| `default`（既定） | 既存シナリオ。互換維持 |
| `split` | 大画面 + 画面分割を含む混合シナリオ |
| `largefile` | 大ファイル向けのページ/末尾までスクロール重視 |

引数はサブコマンド形式ではなく、`--mode <name>` のオプション形式とし、
イテレーション回数は従来通り位置引数で受ける。

### ランナーの拡張

`bench/run-profile.sh` を以下のように拡張する。

```
bash bench/run-profile.sh <iterations> [mode] [testdata]
```

- 第1引数: イテレーション数（既存）
- 第2引数: シナリオモード（`default` / `split` / `largefile`、省略時 `default`）
- 第3引数: テストデータパス（省略時はモード既定値）
- 環境変数は前節「環境変数体系」表に準ずる

モード既定の testdata と画面サイズ:

| モード | testdata 既定 | 画面サイズ既定（環境変数未指定時） |
|---|---|---|
| `default` | `bench/testdata.txt`（10,000行） | 80x24 |
| `split` | `bench/testdata.txt`（10,000行） | 200x60 |
| `largefile` | `bench/testdata-100k.txt`（100,000行） | 200x60 |

testdata が存在しなければ runner が `generate-testdata.sh <lines> <path>`
を呼んで自動生成する。命名規約は `bench/testdata[-<size>].txt` とし、
`bench/testdata*.txt` を `.gitignore` 対象に含める。

出力ファイル名にはモードを含め、複数モードの結果を区別する。

```
bench/results/profile-<mode>-YYYYMMDD-HHMMSS.jfr
bench/results/flamegraph-<mode>-YYYYMMDD-HHMMSS.html
bench/results/collapsed-<mode>-YYYYMMDD-HHMMSS.txt
```

### 大きめテストデータの生成

`bench/generate-testdata.sh` を行数引数 `$1` を受け取れるよう拡張する。
既存呼び出し（引数なし）は 10,000 行を維持し、`100000` を渡すと
10万行のテストデータを生成する。出力先パスは第2引数で指定可能とする。

### 改善対象の判断と受け入れ基準

改善は `default` も含め従来通り `alle` 配下のホットスポットに集中する。
新シナリオ特有のパス（例: `WindowTree`, `WindowLayout`,
`RenderSnapshotFactory` の分割描画経路、大行数での `lineIndexForOffset` 等）
がボトルネックとして浮上した場合、それを優先する。

テストデータに過剰適合（overfitting）した最適化を避けるため、
以下を改善の受け入れ基準とする。

- 改善対象モードで対象メソッドのサンプル数が有意に減少していること
- `default` モードで全サンプル数および主要ホットスポットが有意に劣化
  していないこと（プロファイル誤差範囲内の変動は許容）
- 既存ユニットテストが全件パスし、必要に応じてテストが追加されている
  こと

### 既存結果との非互換性

ターミナルサイズや表示行数が変わるとレンダリングコストの絶対値も
変わるため、新シナリオの結果は ADR 0130 イテレーション1〜4 の
数値とは直接比較しない。新シナリオ内での Before/After 比較に閉じる。

## 決定

- driver / generator / runner / testdata 生成スクリプトを上記方針に従って
  拡張する
- 既存の `default` シナリオは破壊変更を入れず、追加モードとして
  `split` および `largefile` を新設する
- ADR 0130 の改善対象方針（C2 コンパイラ・lanterna I/O・JIT 由来は対象外、
  `alle` 配下のホットスポットに集中）は本ADRでも踏襲する

## 結果

### イテレーション1: GapTextModel の改行位置キャッシュをコードポイント単位でも保持

#### 計測条件

- 3モード（`default` / `split` / `largefile`）で 500 イテレーション
- `largefile` のみ `BENCH_INIT_STABLE_SEC=2.0 BENCH_INIT_WAIT=120`
- async-profiler 4.4、profile_duration=15s

#### Before プロファイル分析

`largefile` モードで以下のメソッドがホットスポットとして浮上した。

- `TextBuffer.lineStartOffset` 経由の `GapTextModel.lineStartOffset` → `charOffsetToCodePointIndex` (= `Character.codePointCount(0, charOffset)`) : 76 サンプル
- `TextBuffer.lineIndexForOffset` 経由の `GapTextModel.lineIndexForOffset` → `toCharOffset` → `offsetCodePoints` : 104 サンプル

いずれも先頭から線形に走査する O(charOffset) または O(codePointIndex) 処理で、
100,000 行ファイルの末尾近くでは毎回数百万 char ぶん走査するため支配的になっていた。

呼び出し元の主なパス:

- `BuiltinStatusLineSlots.renderColumnNumber` → `BufferFacade.lineStartOffset`
- `ScrollUpCommand.execute` → `BufferFacade.lineStartOffset`
- `RenderSnapshotFactory.computeActiveCursorPosition` → `BufferFacade.lineIndexForOffset`

#### 改善内容

`GapTextModel` の改行位置キャッシュ `lineBreakOffsets`（char オフセット）
と並行して、`lineBreakCodePointOffsets`（コードポイントオフセット）を
保持する。両系列は要素数・順序が常に一致し、insert/delete 時に差分更新する。

- `lineStartOffset(lineIndex)`: `lineBreakCodePointOffsets.get(lineIndex - 1) + 1` で O(1)
- `lineIndexForOffset(offset)`: コードポイント系列に対して直接二分探索で O(log N)
- 初期化（`buildLineBreakCaches`）: 一度の走査で char/codePoint 両系列とコードポイント長を計算
- insert/delete のキャッシュ更新は両系列を同期維持

サロゲートペアと改行が混在するケース、コンストラクタ経由初期化のケース、
削除のケースをカバーする 3 件のテストを追加。

#### After 比較（500 イテレーション）

| モード | Before 全 | After 全 | Before alle | After alle |
|---|---|---|---|---|
| default | 354 | 341 | 127 | 119 |
| split | 527 | 512 | 186 | 181 |
| largefile | 731 | **250** | 440 | **80** |

対象メソッド（largefile）:

| メソッド | Before | After |
|---|---|---|
| `TextBuffer.lineStartOffset` | 76 | 3 |
| `TextBuffer.lineIndexForOffset` | 104 | 0 |
| `GapTextModel.lineStartOffset` | 32 | 0 |
| `GapTextModel.lineIndexForOffset` | 59 | 0 |
| `GapTextModel.offsetCodePoints` | 59 | 3 |
| `GapTextModel.charOffsetToCodePointIndex` | 32 | 0 |
| `GapTextModel.toCharOffset` | 59 | 3 |

#### 受け入れ基準への適合

- 改善対象モード（`largefile`）で対象メソッドのサンプル数が有意に減少:
  全体 731→250 (-66%)、alle 440→80 (-82%)、`lineStartOffset` 76→3、
  `lineIndexForOffset` 104→0
- `default` 全 354→341、alle 127→119 で劣化なし（プロファイル誤差内に
  収まる微減）
- `split` 全 527→512、alle 186→181 で劣化なし
- 既存ユニットテストが全件パスし、サロゲートペアと改行混在ケースの
  テストを 3 件追加

#### 既知の制約と次の改善候補

- `updateCacheAfterInsert` の既存改行シフトループは `lineBreakOffsets.size()`
  に対して O(N) のまま。`largefile` で 100k 件あるが、本シナリオは
  スクロール中心で編集が極めて少ないため顕在化していない。大ファイル編集
  シナリオを別途設計した場合のみ再評価する。
- After プロファイルの `largefile` 残存ホットスポットは
  `ScreenRenderer.renderLineAt` / `ScreenRenderer.fillBlank` / lanterna 側
  の文字描画系に移っており、これらは ADR 0130 の改善対象方針の境界上
  （lanterna I/O は対象外、ScreenRenderer は対象内）。
