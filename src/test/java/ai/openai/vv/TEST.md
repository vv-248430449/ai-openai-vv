## 测试命令与分层

本机已装 Maven，直接用 `mvn` 即可（Windows cmd / PowerShell / Git Bash 都行）。
仓库按「测试分层」约定组织，避免每次跑测试都烧大模型 API 额度。

### 测试分层（JUnit 5 @Tag 约定）
| Tag | 含义 | 是否调模型 | 如何跑 |
|---|---|---|---|
| `unit` | 纯逻辑单测（EvalScorerTest 等） | 否，零成本 | 本地 `mvn test` 默认跑 |
| `eval` | 真·大模型评估（AiEvalTest） | 是，花钱 | 仅 CI（带 key）或 test-eval / test-eval-mock 脚本 |

pom 用 surefire `<excludedGroups>${skip.eval.group}</excludedGroups>`，
默认值 `eval` → 本地 `mvn test` 自动排除 eval 组，免费只跑 unit。
CI 用 `mvn -B verify -Dskip.eval.group=` 清空该值，跑全部（含 eval + JaCoCo 门禁）。

### eval 用例数据化（B）
`AiEvalTest` 的用例不再 hardcode 在 Java 里，而是放在
`src/test/resources/eval-cases.json`：
```json
[
  { "input": "1+1 等于几？只回答数字。", "expect": "2", "scorer": "L1" }
]
```
- `input`  ：喂给 SUT（被测系统，这里是 ChatClient）的问题
- `expect` ：期望输出里包含的关键词（Lv1 子串匹配判定用）
- `scorer`  ：评分器级别（L1 规则 / L2 LLM-judge / L3 金标准），缺省 L1

**加一条 eval 用例 = 在 JSON 里加一行，不用改 Java。** 这是 eval 三段式里 Cases 组件的落法。

### 本地可控真 eval（C）
不想每次都烧钱，有两个开关（都是 `AiEvalTest` 的系统属性）：

- `-Deval.mock=true`：**离线模式**，SUT 直接返回 expect，不调模型、0 成本。
  用来验证「循环 + 打分 + 断言」链路本身有没有坏（plumbing smoke test）。
- `-Deval.budget.tokens=N`：**token 预算守卫**，真实 eval 时累计 token 用量，
  超过 N 立即标红（0 = 不限制，默认）。防手滑一次跑太多烧爆额度。

### 一键脚本（推荐）
项目根 `scripts/` 目录下：

| 平台 | 纯单测（免费） | 真·eval（花钱） | 离线 mock eval（0 成本） |
|---|---|---|---|
| Windows | `scripts\test.bat` | `scripts\test-eval.bat` | `scripts\test-eval-mock.bat` |
| Linux/macOS | `bash scripts/test.sh` | `bash scripts/test-eval.sh` | `bash scripts/test-eval-mock.sh` |

- `test.bat` / `test.sh`：跑全部 `unit` 单测，零成本，写码时高频用。
- `test-eval.bat` / `test-eval.sh`：跑 `AiEvalTest` 真·模型评估，前置 `OPENAI_KEY`。
  可加 `-Deval.budget.tokens=5000` 设预算。
- `test-eval-mock.bat` / `test-eval-mock.sh`：离线跑 eval 链路，0 成本，无需 key。

### 手动等价命令
- 本地纯单测（免费）：`mvn test`
- 真·模型 eval（花钱）：`mvn test -Dtest=AiEvalTest -Dskip.eval.group=`
- 离线 mock eval（0 成本）：`mvn test -Dtest=AiEvalTest -Dskip.eval.group= -Deval.mock=true`
- 跑全部（含 eval，CI 用）：`mvn verify -Dskip.eval.group=`

### 分工说明
- `AiEvalTest`（`@Tag eval`）：管「模型答得对不对」（正确性），走 eval-cases.json。
- `AiControllerEvalTest`：管「Controller 接线对不对」（HTTP 层把消息交给 ChatClient 的契约），
  用 Mockito 深桩固定返回，纯覆盖率驱动样本，不读 eval-cases.json。两者分工不混。

CI（.github/workflows/ci.yml）用 `mvn -B verify -Dskip.eval.group=`，本地与云端底层命令一致。
