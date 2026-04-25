---
name: performance-tuning
description: async-profiler によるパフォーマンスプロファイリングと改善のイテレーションを実行する
allowed-tools: Bash, Read, Write, Edit, Glob, Grep, Agent
---

# Performance Tuning

async-profiler を用いた E2E パフォーマンスプロファイリングと改善のイテレーションを実行するスキル。

## 前提

- ADR: `docs/adr/0130-performance-profiling.md`
- ベンチマーク基盤: `bench/` ディレクトリ
- async-profiler 配置先: `tmp/async-profiler-4.4-linux-x64/`

## 引数

| 引数 | 意味 |
|------|------|
| なし（デフォルト） | 1イテレーション（プロファイル取得→分析→改善→再プロファイル） |
| `profile` | プロファイル取得と分析のみ（改善は行わない） |
| `N` (数値) | キーストロークのイテレーション数を指定してプロファイル取得（デフォルト500） |

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
bash bench/run-profile.sh ${ITERATIONS:-500}
```

- Bash ツールの `timeout` は `300000`（5分）に設定すること。ビルドとプロファイリングに時間がかかる。
- 出力される collapsed ファイルのパスを記録しておく。

### Step 3: プロファイル分析

collapsed 形式のファイルを分析する。

```bash
COLLAPSED=bench/results/collapsed-YYYYMMDD-HHMMSS.txt

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
- テストデータに overfitting した最適化をしないこと

### Step 4: 改善実装（`profile` 引数の場合はスキップ）

1イテレーションにつき **1つのボトルネック** に集中する。

1. 改善前に既存テストが全件パスすることを確認する
2. 改善を実装する
3. テストが不足していれば追加する
4. 全テスト実行 + `./gradlew spotlessApply` でビルドが通ることを確認する
5. コミットする

### Step 5: 改善後の再プロファイル

```bash
bash bench/run-profile.sh ${ITERATIONS:-500}
```

改善前後の collapsed を比較する。

```bash
BEFORE=bench/results/collapsed-BEFORE.txt
AFTER=bench/results/collapsed-AFTER.txt

echo "=== 全サンプル数 ==="
echo -n "Before: "; awk '{sum+=$NF} END{print sum}' "$BEFORE"
echo -n "After:  "; awk '{sum+=$NF} END{print sum}' "$AFTER"

echo ""
echo "=== 対象メソッド ==="
echo -n "Before: "; grep '対象メソッド' "$BEFORE" | awk '{sum+=$NF} END{print sum+0}'
echo -n "After:  "; grep '対象メソッド' "$AFTER" | awk '{sum+=$NF} END{print sum+0}'
```

### Step 6: 安全性評価

- 実装安全性評価エージェント（implementation-safety-checker）で評価する
- HIGH/CRITICAL の指摘があれば修正して再評価する

