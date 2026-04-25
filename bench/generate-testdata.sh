#!/bin/bash
# ベンチマーク用テストデータを生成する
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT="${SCRIPT_DIR}/testdata.txt"
LINES=${1:-10000}

echo "Generating ${LINES} lines of test data..."

{
    for i in $(seq 1 "$LINES"); do
        printf "Line %05d: The quick brown fox jumps over the lazy dog. Lorem ipsum dolor sit amet.\n" "$i"
    done
} > "$OUTPUT"

echo "Generated: ${OUTPUT} ($(wc -l < "$OUTPUT") lines, $(wc -c < "$OUTPUT") bytes)"
