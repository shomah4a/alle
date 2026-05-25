---
name: performance-tuning
description: async-profiler によるパフォーマンスプロファイリングと改善のイテレーションを実行する
allowed-tools: Bash, Read, Write, Edit, Glob, Grep, Agent
---

# Performance Tuning

async-profiler を用いた E2E パフォーマンスプロファイリングと改善のイテレーションを実行するスキル。

## 前提

- ADR: `docs/adr/0130-performance-profiling.md`
- シナリオ拡張ADR: `docs/adr/0138-perf-scenario-largescreen-split-largefile.md`
- ベンチマーク基盤: `bench/` ディレクトリ
- async-profiler 配置先: `tmp/async-profiler-4.4-linux-x64/`

## 引数

ARGUMENTS は半角空白区切りのトークン列として解釈する。各トークンを以下のルールで
分類し、シェル変数 `ITERATIONS` / `MODE` / `PROFILE_ONLY` / `MODES` に展開する。

| トークン形式 | 解釈先 | 例 |
|---|---|---|
| 整数 | `ITERATIONS`（既定 `500`） | `500` |
| `profile` | `PROFILE_ONLY=1`（プロファイル取得と分析のみ、改善はスキップ） | `profile` |
| `mode=<name>` | `MODE`（既定 `default`、`<name>` は `default` / `split` / `largefile`） | `mode=split` |
| `modes=<n1>,<n2>,...` | `MODES`（複数モードを順次実行。指定時は `MODE` より優先） | `modes=default,split,largefile` |
| 自然文 | 上記に該当しない指示は文脈として読み取り、必要なら確認に回す | `前回セッションの続き` |

複数引数の組み合わせ例:

- `500 mode=split` — split モード 500 イテレーション
- `profile mode=largefile` — largefile モードでプロファイル取得のみ
- `modes=default,split,largefile` — 3モードを順次実行（イテレーション既定）

トークンに該当する値がなければ既定値を使う。`mode` と `modes` が両方指定された場合は
`modes` を採用し、ユーザーに整合の確認をとる。`ITERATIONS` `MODE` `PROFILE_ONLY` は
以降の Step 2 / Step 5 のコマンドでそのまま参照する。`MODES` が設定されている場合は
Step 2 / Step 5 を各モードについて順次実行する。

## シナリオモード

詳細は `bench/README.md` および ADR 0138 を参照。

| mode | 主目的 | testdata 既定 | 既定画面サイズ |
|---|---|---|---|
| `default` | 単一ウィンドウでのスクロール・編集（ADR 0130 互換） | `bench/testdata.txt` (10,000行) | 80x24 |
| `split` | 大画面 + 画面分割を含む混合シナリオ | `bench/testdata.txt` (10,000行) | 200x60 |
| `largefile` | 大ファイルでのページスクロール中心 | `bench/testdata-100k.txt` (100,000行) | 200x60 |

ターミナルサイズや初期化判定・送信レートは `BENCH_COLS` / `BENCH_ROWS` /
`BENCH_INIT_WAIT` / `BENCH_INIT_STABLE_SEC` / `BENCH_SLEEP_PER_CHUNK` /
`BENCH_DURATION_MARGIN` 等の環境変数で上書き可能。詳細は ADR 0138 の
「環境変数体系」表を参照。

## 実行手順

### Step 0: ブランチの準備

現在のブランチが main の場合、作業用ブランチを作成してチェックアウトする。

```bash
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" = "main" ]; then
    BRANCH_NAME="performance-tuning/$(date +%Y%m%d-%H%M%S)"
    git checkout -b "$BRANCH_NAME"
fi
```

main 以外のブランチにいる場合はそのまま作業を続ける。

### Step 1: async-profiler の準備

`tmp/async-profiler-4.4-linux-x64/bin/asprof` が存在しない場合、以下の手順でダウンロード・展開する。

```bash
PROFILER_VERSION="4.4"
PROFILER_DIR="tmp/async-profiler-${PROFILER_VERSION}-linux-x64"
PROFILER_TAR="tmp/async-profiler-${PROFILER_VERSION}-linux-x64.tar.gz"

if [ ! -f "${PROFILER_DIR}/bin/asprof" ]; then
    mkdir -p tmp
    curl -fSL "https://github.com/async-profiler/async-profiler/releases/download/v${PROFILER_VERSION}/async-profiler-${PROFILER_VERSION}-linux-x64.tar.gz" -o "$PROFILER_TAR"
    tar xzf "$PROFILER_TAR" -C tmp/
fi
```

### Step 2: プロファイル取得

```bash
bash bench/run-profile.sh ${ITERATIONS:-500} ${MODE:-default}
```

- Bash ツールの `timeout` は `300000`（5分）に設定すること。ビルドとプロファイリングに時間がかかる。
- `largefile` モードは初期化と末尾までのスクロールに更に時間がかかる。
  必要に応じ `BENCH_INIT_WAIT=120` 等で延長する。
- 出力は `bench/results/profile-<mode>-<timestamp>.{jfr,html,txt}` 形式で
  モード名がファイル名に含まれる。collapsed のパスを記録しておく。

### Step 3: プロファイル分析

collapsed 形式のファイルを分析する。

```bash
COLLAPSED=bench/results/collapsed-<mode>-YYYYMMDD-HHMMSS.txt

# 全サンプル数
awk '{sum+=$NF} END{print sum}' "$COLLAPSED"

# サンプル数降順
sort -t' ' -k2 -rn "$COLLAPSED" | head -30

# alle パッケージ内サンプル集計
grep 'alle' "$COLLAPSED" | awk '{sum+=$NF} END{print sum, "samples"}'
```

分析時の注意点:

- C2コンパイラ（`CompileBroker`）と lanterna I/O（`__libc_write`）は JIT/IO であり改善対象外
- `alle` パッケージ内のメソッドに絞って、サンプル数が多い箇所を特定する
- 呼び出し元（どのコマンドやレンダリング処理から呼ばれているか）も確認する
- 新シナリオ特有の経路（`split`: `WindowTree` / `WindowLayout` / 分割描画、
  `largefile`: 大行数での `lineIndexForOffset` 等）に注目する
- テストデータに overfitting した最適化をしないこと

### Step 4: 改善実装（`profile` 引数の場合はスキップ）

1イテレーションにつき **1つのボトルネック** に集中する。

1. 改善前に既存テストが全件パスすることを確認する
2. 改善を実装する
3. テストが不足していれば追加する
4. 全テスト実行 + `./gradlew spotlessApply` でビルドが通ることを確認する
5. コミットする

#### 改善の受け入れ基準（ADR 0138）

- 改善対象モードで対象メソッドのサンプル数が有意に減少していること
- `default` モードで全サンプル数および主要ホットスポットが有意に劣化
  していないこと（モード横断の非劣化チェック）
- 既存ユニットテストが全件パスし、必要に応じてテストが追加されている
  こと

### Step 5: 改善後の再プロファイル

```bash
bash bench/run-profile.sh ${ITERATIONS:-500} ${MODE:-default}
```

改善前後の collapsed を比較する。

```bash
BEFORE=bench/results/collapsed-<mode>-BEFORE.txt
AFTER=bench/results/collapsed-<mode>-AFTER.txt

echo "=== 全サンプル数 ==="
echo -n "Before: "; awk '{sum+=$NF} END{print sum}' "$BEFORE"
echo -n "After:  "; awk '{sum+=$NF} END{print sum}' "$AFTER"

echo ""
echo "=== 対象メソッド ==="
echo -n "Before: "; grep '対象メソッド' "$BEFORE" | awk '{sum+=$NF} END{print sum+0}'
echo -n "After:  "; grep '対象メソッド' "$AFTER" | awk '{sum+=$NF} END{print sum+0}'
```

改善対象モードと異なるモード（特に `default`）でも非劣化を確認する。

### Step 6: 安全性評価

- 実装安全性評価エージェント（implementation-safety-checker）で評価する
- HIGH/CRITICAL の指摘があれば修正して再評価する
