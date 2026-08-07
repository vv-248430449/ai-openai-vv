@echo off
setlocal
REM ============================================================================
REM Real LLM eval: run AiEvalTest with a real model call (costs tokens/API fee).
REM Prerequisites: Maven on PATH (mvn -v); OPENAI_KEY env var or CI secret set.
REM Optional budget guard: append  -Deval.budget.tokens=N  (0 = no limit, default)
REM Debug: pass "debug" as 1st arg to suspend for a remote debugger on port 5005
REM        (IDEA: Run/Debug Configurations -> Remote JVM Debug -> localhost:5005 -> Attach)
REM Usage: scripts\test-eval.bat [debug]
REM ============================================================================
set DEBUG_FLAG=
if /I "%1"=="debug" set DEBUG_FLAG=-Dmaven.surefire.debug

echo [INFO] Running real LLM eval (costs tokens): AiEvalTest
mvn test -Dtest=AiEvalTest -Dskip.eval.group= %DEBUG_FLAG%
if errorlevel 1 (
    echo [FAIL] eval test failed. See Maven output above.
    pause
    exit /b 1
)
echo [PASS] eval test passed.
pause
