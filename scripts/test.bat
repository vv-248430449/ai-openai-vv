@echo off
setlocal
REM ============================================================================
REM One-click PURE-LOGIC unit tests (no LLM call, zero cost).
REM Prereq: Maven on PATH (verify with: mvn -v).
REM Runs only @Tag("unit") tests (EvalScorerTest); @Tag("eval") excluded by default.
REM Double-click works; window pauses at end to show results.
REM ============================================================================
echo [INFO] Running unit tests (no LLM eval, free)...
mvn test
if errorlevel 1 (
    echo [FAIL] unit tests failed. See Maven output above.
    pause
    exit /b 1
)
echo [PASS] unit tests passed.
pause
