## 本地测试命令

本机已装 Maven，直接用 `mvn` 即可（Windows cmd / PowerShell / Git Bash 都行）。
另外提供了**一键脚本**，不用每次手敲长命令。

### 一键脚本（推荐）

项目根 `scripts/` 目录下，按平台直接用对应脚本：

| 平台 | 命令 | 说明 |
|---|---|---|
| Windows | `scripts\test-eval.bat` | 在 cmd / IDEA Terminal / 双击均可运行；双击时窗口会暂停以便看结果 |
| Linux / macOS | `bash scripts/test-eval.sh` | 或直接 `./scripts/test-eval.sh`（需可执行权限） |

脚本默认只跑 LLM eval 类 `ai.openai.vv.AiEvalTest#evalLlmAnswers`。
前置：已配置环境变量 `OPENAI_KEY`（否则测试会直接标红 FAIL）。

### 手动等价命令（如需自定义参数）

- 只跑 LLM eval 类：
  mvn test -Dtest=ai.openai.vv.AiEvalTest#evalLlmAnswers

- 跑全部测试：
  mvn test

CI（.github/workflows/ci.yml）用的是 `mvn -B test`（跑全部），本地与云端命令一致。
