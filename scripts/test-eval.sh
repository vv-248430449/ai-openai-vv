#!/usr/bin/env bash
set -euo pipefail
# Real LLM eval: run AiEvalTest with a real model call (costs tokens/API fee).
# Prerequisites: Maven on PATH; OPENAI_KEY env var set.
# Optional budget guard: append  -Deval.budget.tokens=N  (0 = no limit, default)
echo "[INFO] Running real LLM eval (costs tokens): AiEvalTest"
mvn test -Dtest=AiEvalTest -Dskip.eval.group=
echo "[PASS] eval test passed."
