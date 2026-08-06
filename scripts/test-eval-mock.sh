#!/usr/bin/env bash
set -euo pipefail
# Offline mock eval: run AiEvalTest with -Deval.mock=true
# No model call, 0 cost. Validates the "loop + scoring + assertion" plumbing.
echo "[INFO] Offline mock eval (no model call, 0 cost): AiEvalTest"
mvn test -Dtest=AiEvalTest -Dskip.eval.group= -Deval.mock=true
echo "[PASS] mock eval passed."
