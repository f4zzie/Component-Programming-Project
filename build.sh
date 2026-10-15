#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# SCO310 QR Code generator - component suite build script
#
# Deliberately dependency-free: the project compiles with the JDK alone
# (javac as required by step 3 of "8 Steps to Create a JavaBean"), and packages
# the beans into a JAR with a manifest and the optional third-party library
# (step 4-6 of the same list).
#
# usage: ./build.sh [clean|compile|test|jar|docs|all]     (default: all)
# ---------------------------------------------------------------------------
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

LIB_DIR="lib"
CORE_JAR="$LIB_DIR/core-3.5.3.jar"
JAVASE_JAR="$LIB_DIR/javase-3.5.3.jar"
OUT_DIR="out"
CLASSES="$OUT_DIR/classes"
TEST_CLASSES="$OUT_DIR/test-classes"
DIST="$OUT_DIR/dist"
JAR_NAME="qr-component-suite-1.0.0.jar"
RELEASE="11"

CP_MAIN="$CLASSES:$CORE_JAR:$JAVASE_JAR"
CP_TEST="$TEST_CLASSES:$CP_MAIN"

info() { printf '\033[1;34m[build]\033[0m %s\n' "$*"; }

clean() {
  info "removing build output"
  find "$OUT_DIR" -mindepth 1 -delete
  mkdir -p "$CLASSES" "$TEST_CLASSES" "$DIST"
}

compile_main() {
  info "compiling main sources with javac (--release $RELEASE)"
  mkdir -p "$CLASSES"
  # -Xlint:all keeps every useful diagnostic. this-escape is switched off on purpose: the
  # JavaBeans bound/constrained property pattern requires new PropertyChangeSupport(this) in a
  # field initialiser, and the helper only stores the reference without calling back into the bean.
  javac -Xlint:all,-this-escape -cp "$CORE_JAR" -d "$CLASSES" --release "$RELEASE" \
        $(find src/main/java -name '*.java')
  info "main classes: $(find "$CLASSES" -name '*.class' | wc -l)"
}

compile_tests() {
  info "compiling the verification suite"
  mkdir -p "$TEST_CLASSES"
  javac -cp "$CORE_JAR:$JAVASE_JAR" -d "$TEST_CLASSES" --release "$RELEASE" \
        $(find src/main/java src/test/java -name '*.java')
}

run_verification() {
  compile_tests
  info "running the automated verification suite"
  java -cp "$CP_TEST:$JAVASE_JAR" ke.ku.sco310.qr.test.VerificationRunner docs/evidence
}

run_mask_comparison() {
  info "comparing the mask-selection stage with the independent ZXing encoder"
  mkdir -p "$OUT_DIR/tools"
  javac -cp "$CLASSES:$CORE_JAR" -d "$OUT_DIR/tools" --release "$RELEASE" tools/MaskComparison.java
  java -cp "$OUT_DIR/tools:$CLASSES:$CORE_JAR" MaskComparison docs/evidence/mask-selection-comparison.csv
}

package_jar() {
  compile_main
  info "packaging the components into $JAR_NAME (manifest: manifest/MANIFEST.MF)"
  cp config/assembly.xml "$CLASSES/assembly-defaults.xml"
  jar --create --file "$DIST/$JAR_NAME" --manifest manifest/MANIFEST.MF -C "$CLASSES" .
  info "verifying the archive"
  jar --list --file "$DIST/$JAR_NAME" | head -5
  unzip -p "$DIST/$JAR_NAME" META-INF/MANIFEST.MF
  info "jar written: $DIST/$JAR_NAME ($(stat -c%s "$DIST/$JAR_NAME") bytes)"
  mkdir -p "$DIST/lib"
  cp "$CORE_JAR" "$JAVASE_JAR" "$DIST/lib/" 2>/dev/null || true
}

build_docs() {
  info "regenerating the documentation"
  python3 tools/make_documentation.py
}

case "${1:-all}" in
  clean)   clean ;;
  compile) compile_main ;;
  test)    run_verification ;;
  compare) compile_main; run_mask_comparison ;;
  jar)     package_jar ;;
  docs)    build_docs ;;
  all)
    clean
    compile_main
    run_verification
    run_mask_comparison
    package_jar
    ;;
  *) echo "unknown target: $1"; exit 2 ;;
esac

info "done"
