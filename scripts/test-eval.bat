@echo off
setlocal

REM ============================================================================
REM One-click LLM eval test: ai.openai.vv.AiEvalTest#evalLlmAnswers
REM Prerequisites:
REM   1. Maven installed and on PATH (verify with: mvn -v)
REM   2. OPENAI_KEY environment variable set (otherwise test fails red)
REM Usage:
REM   cmd / PowerShell / IDEA Terminal:  scripts\test-eval.bat
REM   Double-click also works; window pauses at end to show results.
REM   To skip pause in a terminal, delete the final "pause" line.
REM ============================================================================

echo [INFO] Running LLM eval test: AiEvalTest#evalLlmAnswers
mvn test -Dtest=ai.openai.vv.AiEvalTest#evalLlmAnswers
if errorlevel 1 (
    echo [FAIL] eval test failed. See Maven output above.
    pause
    exit /b 1
)

echo [PASS] eval test passed.
pause
