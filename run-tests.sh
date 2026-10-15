#!/usr/bin/env bash
# Runs the automated verification suite and refreshes the evidence folder.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
./build.sh test
