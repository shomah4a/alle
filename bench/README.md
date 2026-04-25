# パフォーマンスプロファイリング手順

## 前提

- async-profiler が `tmp/async-profiler-4.4-linux-x64/` に配置されていること
- ブランチ `feature/performance-profiling` で作業すること
- ADR: `docs/adr/0130-performance-profiling.md`

## ディレクトリ構成

```
bench/
├── README.md              # この文書
├── generate-testdata.sh   # テストデータ生成（10,000行、850KB）
├── generate-keystrokes.py # キーストローク生成（C-p/C-n スクロール重視）
├── driver.py              # pty経由でエディタを起動しプロファイルを取得するドライバー
├── run-profile.sh         # 一連の流れを実行するラッパー
├── testdata.txt           # 生成済みテストデータ（gitignore推奨）
└── results/               # プロファイル結果（gitignore推奨）
    ├── profile-YYYYMMDD-HHMMSS.jfr
    ├── flamegraph-YYYYMMDD-HHMMSS.html
    └── collapsed-YYYYMMDD-HHMMSS.txt
```

## イテレーション手順

各イテレーションは以下の手順で進める。

### 1. プロファイル取得

```bash
bash bench/run-profile.sh 500
```

- shadow jar をビルド
- 10,000行のテストファイルを開き、500イテレーション分のキーストローク（主にC-p/C-n スクロール）を送信
- `asprof collect -d <秒数>` で初期化後のCPUプロファイルを JFR 形式で取得
- `jfrconv` で HTML フレームグラフと collapsed 形式に変換
- 結果は `bench/results/` に出力される

引数の数値はキーストロークのイテレーション数。デフォルト500。

### 2. プロファイル分析

collapsed 形式をサンプル数降順でソートして上位を確認する。

```bash
COLLAPSED=bench/results/collapsed-YYYYMMDD-HHMMSS.txt

# 全サンプル数
awk '{sum+=$NF} END{print sum}' "$COLLAPSED"

# サンプル数降順
sort -t' ' -k2 -rn "$COLLAPSED" | head -30

# 特定メソッドのサンプル集計
grep 'メソッド名' "$COLLAPSED" | awk '{sum+=$NF} END{print sum, "samples"}'
```

HTML フレームグラフはブラウザで開いてインタラクティブに分析可能。

### 3. ボトルネック特定

分析時に注目すべき点:

- C2コンパイラ（`CompileBroker`）と lanterna I/O（`__libc_write`）は JIT/IO であり改善対象外
- `alle` パッケージ内のメソッドに絞って、サンプル数が多い箇所を特定する
- 呼び出し元（どのコマンドやレンダリング処理から呼ばれているか）も確認する

### 4. 改善実装

- 1イテレーションにつき **1つのボトルネック** に集中する
- 改善前に既存テストが全件パスすることを確認する
- 改善を実装する
- 全テスト実行 + spotlessApply でビルドが通ることを確認する
- コミットする

### 5. 改善後の再プロファイル

```bash
bash bench/run-profile.sh 500
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

### 6. 安全性評価

- 実装安全性評価エージェント（implementation-safety-checker）で評価
- HIGH/CRITICAL の指摘があれば修正して再評価
- Codex レビューも実施: `/codex-review branch`

### 7. ADR 更新

`docs/adr/0130-performance-profiling.md` にイテレーション結果を追記する。

## 技術的な注意事項

### pty ドライバーについて

- lanterna の `UnixTerminal` は `/dev/tty` と stty を必要とする
- `driver.py` が Python の `pty` モジュールで疑似端末を割り当て、`setsid()` + `TIOCSCTTY` で制御端末を設定する
- 画面出力が 1000 バイトを超えたら初期化完了とみなし、2秒待ってからプロファイリングを開始する
- GraalPy の初期化に約25秒かかるため、初期化タイムアウトは60秒に設定

### キーストロークシナリオ

`generate-keystrokes.py` が生成するシナリオ:

1. C-n (next-line) × iterations — 下方向スクロール
2. C-p (previous-line) × iterations — 上方向スクロール
3. C-v / M-v × iterations/10 — ページスクロール
4. C-a / C-e × iterations/2 — 行頭/行末移動
5. C-f / C-b × iterations/2 — 文字単位移動
6. テキスト挿入 × iterations/10
7. C-d 削除 × iterations/10
8. C-q で終了

ユーザーの体感（大きめファイルでの C-p/C-n スクロールが遅い）を再現するため、スクロール操作を重点的に含む。

## イテレーション1の結果（参考）

| 指標 | Before | After | 改善率 |
|---|---|---|---|
| 全サンプル数 | 2,426 | 1,003 | -59% |
| GapTextModel.length() | 1,006 | 0 | -100% |
| Character.codePointCount | 1,007 | 21 | -98% |
| GapTextModel.lineIndexForOffset | 725 | 8 | -99% |
| RenderSnapshotFactory | 1,218 | 120 | -90% |

改善内容: `GapTextModel.length()` を O(n) → O(1) に変更（`codePointLength` フィールドキャッシュ）。
