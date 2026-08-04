## 测试命令与分层

本机已装 Maven，直接用 `mvn` 即可（Windows cmd / PowerShell / Git Bash 都行）。
仓库按「测试分层」约定组织，避免每次跑测试都烧大模型 API 额度。

### 测试分层（JUnit 5 @Tag 约定）
| Tag | 含义 | 是否调模型 | 如何跑 |
|---|---|---|---|
| `unit` | 纯逻辑单测（EvalScorerTest 等） | 否，零成本 | 本地 `mvn test` 默认跑 |
| `eval` | 真·大模型评估（AiEvalTest） | 是，花钱 | 仅 CI（带 key）或 test-eval 脚本 |

pom 用 surefire `<excludedGroups>${skip.eval.group}</excludedGroups>`，
默认值 `eval` → 本地 `mvn test` 自动排除 eval 组，免费只跑 unit。
CI 用 `mvn -B test -Dskip.eval.group=` 清空该值，跑全部（含 eval）。

### 一键脚本（推荐）
项目根 `scripts/` 目录下：

| 平台 | 纯单测（免费） | 真·eval（花钱） |
|---|---|---|
| Windows | `scripts\test.bat` | `scripts\test-eval.bat` |
| Linux/macOS | `bash scripts/test.sh` | `bash scripts/test-eval.sh` |

- `test.bat` / `test.sh`：跑全部 `unit` 单测，零成本，写码时高频用。
- `test-eval.bat` / `test-eval.sh`：跑 `AiEvalTest` 真·模型评估，前置 `OPENAI_KEY`。

### 手动等价命令
- 本地纯单测（免费）：`mvn test`
- 真·模型 eval（花钱）：`mvn test -Dtest=ai.openai.vv.AiEvalTest -Dskip.eval.group=`
- 跑全部（含 eval，CI 用）：`mvn test -Dskip.eval.group=`

CI（.github/workflows/ci.yml）用 `mvn -B test -Dskip.eval.group=`，本地与云端底层命令一致。
