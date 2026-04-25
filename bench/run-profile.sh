#!/bin/bash
# async-profiler 付きでエディタをE2E実行し、プロファイルを取得する
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PROFILER_DIR="$PROJECT_DIR/tmp/async-profiler-4.4-linux-x64"
PROFILER_LIB="$PROFILER_DIR/lib/libasyncProfiler.so"
ASPROF_BIN="$PROFILER_DIR/bin/asprof"
JFRCONV_BIN="$PROFILER_DIR/bin/jfrconv"
OUTPUT_DIR="$SCRIPT_DIR/results"
TESTDATA="$SCRIPT_DIR/testdata.txt"
FAT_JAR="$PROJECT_DIR/alle-app/build/libs/alle-app-0.1.0-SNAPSHOT-all.jar"
ITERATIONS=${1:-500}

mkdir -p "$OUTPUT_DIR"

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
JFR_FILE="$OUTPUT_DIR/profile-${TIMESTAMP}.jfr"
FLAMEGRAPH="$OUTPUT_DIR/flamegraph-${TIMESTAMP}.html"
COLLAPSED="$OUTPUT_DIR/collapsed-${TIMESTAMP}.txt"

# テストデータが存在しなければ生成
if [ ! -f "$TESTDATA" ]; then
    echo "Generating test data..."
    bash "$SCRIPT_DIR/generate-testdata.sh"
fi

echo "Building shadow jar..."
cd "$PROJECT_DIR"
./gradlew :alle-app:shadowJar -q

echo "Generating keystrokes (iterations=$ITERATIONS)..."
KEYSTROKE_FILE=$(mktemp)
python3 "$SCRIPT_DIR/generate-keystrokes.py" "$ITERATIONS" > "$KEYSTROKE_FILE"
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
echo "JFR:        $JFR_FILE"
echo "Flamegraph: $FLAMEGRAPH"
echo "Collapsed:  $COLLAPSED"
