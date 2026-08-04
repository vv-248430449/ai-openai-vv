#!/usr/bin/env bash
set -euo pipefail
# One-click PURE-LOGIC unit tests (no LLM call, zero cost).
# Runs only @Tag("unit") tests; @Tag("eval") excluded by default.
echo "[INFO] Running unit tests (no LLM eval, free)..."
mvn test
echo "[PASS] unit tests passed."
