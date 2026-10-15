#!/usr/bin/env bash
# Web service assembly of the component suite (default port 8099).
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
exec java -cp "out/classes:lib/core-3.5.3.jar:lib/javase-3.5.3.jar" \
     ke.ku.sco310.qr.web.QrHttpServiceComponent "$@"
