@echo off
setlocal
REM ============================================================================
REM Offline mock eval: run AiEvalTest with -Deval.mock=true
REM No model call, 0 cost. Validates the "loop + scoring + assertion" plumbing.
REM Prerequisites: Maven on PATH (verify: mvn -v). No OPENAI_KEY needed.
REM Usage: scripts\test-eval-mock.bat
REM ============================================================================
echo [INFO] Offline mock eval (no model call, 0 cost): AiEvalTest
mvn test -Dtest=AiEvalTest -Dskip.eval.group= -Deval.mock=true
if errorlevel 1 (
    echo [FAIL] mock eval failed. See Maven output above.
    pause
    exit /b 1
)
echo [PASS] mock eval passed.
pause
