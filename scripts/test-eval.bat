@echo off
setlocal
REM ============================================================================
REM One-click LLM eval test: ai.openai.vv.AiEvalTest#evalLlmAnswers
REM Prereq: Maven on PATH + OPENAI_KEY env var set (else test fails red).
REM NOTE: pom excludes @Tag("eval") by default; -Dskip.eval.group= clears that
REM       exclusion so this script actually runs the real model eval.
REM ============================================================================
echo [INFO] Running LLM eval test: AiEvalTest#evalLlmAnswers
mvn test -Dtest=ai.openai.vv.AiEvalTest#evalLlmAnswers -Dskip.eval.group=
if errorlevel 1 (
    echo [FAIL] eval test failed. See Maven output above.
    pause
    exit /b 1
)
echo [PASS] eval test passed.
pause
