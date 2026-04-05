#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

JDTLS_VERSION="1.57.0"
JDTLS_TIMESTAMP="202602261110"
JDTLS_DIR="${REPO_ROOT}/tmp/jdtls"
JDTLS_ARCHIVE="jdt-language-server-${JDTLS_VERSION}-${JDTLS_TIMESTAMP}.tar.gz"
JDTLS_URL="https://www.eclipse.org/downloads/download.php?file=/jdtls/milestones/${JDTLS_VERSION}/${JDTLS_ARCHIVE}"

if [ ! -x "${JDTLS_DIR}/bin/jdtls" ]; then
    echo "jdtls ${JDTLS_VERSION} をダウンロードしています..." >&2
    mkdir -p "${JDTLS_DIR}"
    curl -fSL --progress-bar -o "${JDTLS_DIR}/${JDTLS_ARCHIVE}" "${JDTLS_URL}"
    tar xzf "${JDTLS_DIR}/${JDTLS_ARCHIVE}" -C "${JDTLS_DIR}"
    rm -f "${JDTLS_DIR}/${JDTLS_ARCHIVE}"
    echo "ダウンロード完了" >&2
fi

exec "${JDTLS_DIR}/bin/jdtls" "$@"
