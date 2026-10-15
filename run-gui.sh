#!/usr/bin/env bash
# Desktop assembly of the component suite.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
exec java -cp "out/classes:lib/core-3.5.3.jar:lib/javase-3.5.3.jar" \
     ke.ku.sco310.qr.ui.QrStudioGuiAssembly "$@"
