#!/usr/bin/env bash
# Command line assembly of the component suite.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
exec java -cp "out/classes:out/test-classes:lib/core-3.5.3.jar:lib/javase-3.5.3.jar" \
     ke.ku.sco310.qr.cli.QrCliAssembly "$@"
