#!/bin/bash
# ベンチマーク用テストデータを生成する
#
# 使い方:
#   generate-testdata.sh [LINES] [OUTPUT_PATH]
#
# 既定:
#   LINES        = 10000
#   OUTPUT_PATH  = <script_dir>/testdata.txt
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LINES=${1:-10000}
OUTPUT=${2:-${SCRIPT_DIR}/testdata.txt}

echo "Generating ${LINES} lines of test data into ${OUTPUT}..."

mkdir -p "$(dirname "$OUTPUT")"

awk -v n="$LINES" 'BEGIN {
    for (i = 1; i <= n; i++) {
        printf("Line %05d: The quick brown fox jumps over the lazy dog. Lorem ipsum dolor sit amet.\n", i)
    }
}' > "$OUTPUT"

echo "Generated: ${OUTPUT} ($(wc -l < "$OUTPUT") lines, $(wc -c < "$OUTPUT") bytes)"
