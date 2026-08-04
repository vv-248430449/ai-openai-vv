#!/usr/bin/env bash
set -euo pipefail
# One-click LLM eval test: ai.openai.vv.AiEvalTest#evalLlmAnswers
# Prereq: Maven on PATH + OPENAI_KEY env var set (else test fails red).
# pom excludes @Tag("eval") by default; -Dskip.eval.group= clears it so eval runs.
echo "[INFO] Running LLM eval test: AiEvalTest#evalLlmAnswers"
mvn test -Dtest=ai.openai.vv.AiEvalTest#evalLlmAnswers -Dskip.eval.group=
echo "[PASS] eval test passed."
