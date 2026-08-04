#!/usr/bin/env bash
# ============================================================================
# 运行 LLM eval 测试：ai.openai.vv.AiEvalTest#evalLlmAnswers
# 前置条件�?#   1. 本机已安�?Maven 并加�?PATH（mvn -v 可正常输出版本）
#   2. 已配置环境变�?OPENAI_KEY（否则测试会直接标红 FAIL�?# 用法�?#   bash scripts/test-eval.sh
#   或赋予可执行权限后直接运行：  chmod +x scripts/test-eval.sh && ./scripts/test-eval.sh
# ============================================================================
set -euo pipefail

echo "[INFO] 启动 LLM eval 测试 ..."
mvn test -Dtest=ai.openai.vv.AiEvalTest#evalLlmAnswers
echo "[PASS] eval 测试全部通过"
