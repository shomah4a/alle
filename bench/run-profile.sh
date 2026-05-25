#!/bin/bash
# async-profiler 付きでエディタをE2E実行し、プロファイルを取得する
#
# 使い方:
#   run-profile.sh [iterations] [mode] [testdata]
#
# 引数:
#   iterations  キーストロークのイテレーション数（既定 500）
#   mode        シナリオモード: default | split | largefile（既定 default）
#   testdata    テストデータパス（省略時はモード既定）
#
# 環境変数（driver.py が解釈する）:
#   BENCH_COLS / BENCH_ROWS         ターミナルサイズ
#   BENCH_INIT_WAIT                 初期化待ち最大秒数
#   BENCH_INIT_MIN_BYTES            初期化完了とみなす最小出力バイト数
#   BENCH_INIT_STABLE_SEC           安定判定の待機秒数
#   BENCH_SLEEP_PER_CHUNK           チャンク送信間隔
#   BENCH_DURATION_MARGIN           profile_duration マージン
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PROFILER_DIR="$PROJECT_DIR/tmp/async-profiler-4.4-linux-x64"
PROFILER_LIB="$PROFILER_DIR/lib/libasyncProfiler.so"
ASPROF_BIN="$PROFILER_DIR/bin/asprof"
JFRCONV_BIN="$PROFILER_DIR/bin/jfrconv"
OUTPUT_DIR="$SCRIPT_DIR/results"
FAT_JAR="$PROJECT_DIR/alle-app/build/libs/alle-app-0.1.0-SNAPSHOT-all.jar"

ITERATIONS=${1:-500}
MODE=${2:-default}
TESTDATA_ARG=${3:-}

# モード既定値の決定
case "$MODE" in
    default)
        DEFAULT_TESTDATA="$SCRIPT_DIR/testdata.txt"
        DEFAULT_LINES=10000
        ;;
    split)
        DEFAULT_TESTDATA="$SCRIPT_DIR/testdata.txt"
        DEFAULT_LINES=10000
        # 大画面の既定値（未指定時のみ）
        : "${BENCH_COLS:=200}"
        : "${BENCH_ROWS:=60}"
        export BENCH_COLS BENCH_ROWS
        ;;
    largefile)
        DEFAULT_TESTDATA="$SCRIPT_DIR/testdata-100k.txt"
        DEFAULT_LINES=100000
        : "${BENCH_COLS:=200}"
        : "${BENCH_ROWS:=60}"
        export BENCH_COLS BENCH_ROWS
        ;;
    *)
        echo "ERROR: unknown mode: $MODE (expected default|split|largefile)" >&2
        exit 2
        ;;
esac

TESTDATA=${TESTDATA_ARG:-$DEFAULT_TESTDATA}

mkdir -p "$OUTPUT_DIR"

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
JFR_FILE="$OUTPUT_DIR/profile-${MODE}-${TIMESTAMP}.jfr"
FLAMEGRAPH="$OUTPUT_DIR/flamegraph-${MODE}-${TIMESTAMP}.html"
COLLAPSED="$OUTPUT_DIR/collapsed-${MODE}-${TIMESTAMP}.txt"

# テストデータが存在しなければ生成
if [ ! -f "$TESTDATA" ]; then
    echo "Generating test data: $TESTDATA (${DEFAULT_LINES} lines)..."
    bash "$SCRIPT_DIR/generate-testdata.sh" "$DEFAULT_LINES" "$TESTDATA"
fi

echo "Building shadow jar..."
( cd "$PROJECT_DIR" && ./gradlew :alle-app:shadowJar -q )

echo "Generating keystrokes (mode=$MODE iterations=$ITERATIONS)..."
KEYSTROKE_FILE=$(mktemp)
python3 "$SCRIPT_DIR/generate-keystrokes.py" --mode "$MODE" "$ITERATIONS" > "$KEYSTROKE_FILE"
echo "Keystroke file: $(wc -c < "$KEYSTROKE_FILE") bytes"

echo "Running E2E profile..."
python3 "$SCRIPT_DIR/driver.py" \
    "$FAT_JAR" \
    "$PROFILER_LIB" \
    "$ASPROF_BIN" \
    "$TESTDATA" \
    "$KEYSTROKE_FILE" \
    "$JFR_FILE"

rm -f "$KEYSTROKE_FILE"

echo ""
echo "Converting JFR to flamegraph and collapsed..."
"$JFRCONV_BIN" -o html "$JFR_FILE" "$FLAMEGRAPH"
"$JFRCONV_BIN" -o collapsed "$JFR_FILE" "$COLLAPSED"

echo ""
echo "=== Profile complete ==="
echo "Mode:       $MODE"
echo "Iterations: $ITERATIONS"
echo "Testdata:   $TESTDATA"
echo "JFR:        $JFR_FILE"
echo "Flamegraph: $FLAMEGRAPH"
echo "Collapsed:  $COLLAPSED"
