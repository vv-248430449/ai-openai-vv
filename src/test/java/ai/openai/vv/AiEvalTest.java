package ai.openai.vv;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * ──────────────────────────────────────────────────────────────
 *  最小可用 eval（评估）演示 —— 直接迁移进 ai-openai-vv
 * ──────────────────────────────────────────────────────────────
 *
 * eval 永远逃不出三段式（详见 wiki/programming/eval-three-stage.md）：
 *   1) SUT（被测系统）：本项目里就是 Spring 注入的 ChatClient。
 *   2) Scorer（评分器）：见 RuleScorer（Lv1 规则法，已抽成独立纯逻辑组件）。
 *   3) Cases（用例集）：src/test/resources/eval-cases.json（加用例=加一行 JSON）。
 *
 * 测试分层（本仓库约定）：
 *   - @Tag("unit") ：EvalScorerTest，纯逻辑单测，不调模型、零成本，
 *                    本地 `mvn test` 默认跑（pom 用 excludedGroups 排除 eval）。
 *   - @Tag("eval")  ：本类，真实调用大模型，默认被 pom 的 excludedGroups 排除，
 *                     仅 CI（带 key）或 scripts/test-eval*.bat 显式开启时运行。
 *
 * 跑法：
 *   本地纯单测（免费）：        mvn test                                        （或 scripts\test.bat）
 *   真·模型 eval（花钱）：      mvn test -Dtest=AiEvalTest -Dskip.eval.group=    （或 scripts\test-eval.bat）
 *   离线 mock eval（0 成本）：  mvn test -Dtest=AiEvalTest -Dskip.eval.group= -Deval.mock=true
 *                                                                               （或 scripts\test-eval-mock.bat）
 *
 * 本地可控真 eval（防手滑烧钱）：
 *   -Deval.mock=true         ：不调模型，SUT 直接返回 expect，验证"循环+打分+断言"链路，0 费用。
 *   -Deval.budget.tokens=N   ：真实 eval 时累计 token 用量，超过 N 即标红（0=不限制，默认）。
 *
 * 需要（仅真实 eval 需要）：application.yaml 配置好的【可用】API Key
 *   （本地环境变量 OPENAI_KEY，或 CI 的 secrets.OPENAI_KEY）+ 运行环境能联网。
 *   未配置密钥时真实调用会失败 → 测试标红（宁可红，也不要假绿）。
 * ──────────────────────────────────────────────────────────────
 */
@SpringBootTest
@Tag("eval")
class AiEvalTest {

    /** 以 UTF-8 输出，避免 Windows(GBK) 控制台下 System.out 中文乱码 */
    private static final PrintStream OUT =
            new PrintStream(System.out, true, StandardCharsets.UTF_8);

    /** SUT：Spring 注入的真实 ChatClient（底层指向 application.yaml 配置的模型） */
    @Autowired
    ChatClient chatClient;

    /** Scorer（Lv1 规则法）：已抽成独立组件 RuleScorer，见 EvalScorerTest 的纯逻辑单测 */
    private final RuleScorer scorer = new RuleScorer();

    @Test
    void evalLlmAnswers() throws Exception {
        // Cases：从 JSON 读取（数据化，加用例=加一行 JSON，不改 Java）
        List<EvalCase> cases = EvalCasesLoader.load("eval-cases.json");
        if (cases.isEmpty()) {
            fail("eval-cases.json 为空，没有可评估用例");
        }

        // C-本地可控：mock 离线模式（0 成本验证链路）/ token 预算守卫（仅真实 eval）
        boolean mock = Boolean.parseBoolean(System.getProperty("eval.mock", "false"));
        int budget = Integer.parseInt(System.getProperty("eval.budget.tokens", "0"));
        int used = 0;
        int pass = 0;

        for (EvalCase c : cases) {
            String out;
            if (mock) {
                // 离线模式：SUT 直接返回期望答案，验证"循环+打分+断言" plumbing，不烧 token。
                // （想顺便验证 scorer 归一化，可改成返回中文形式如"二"，这里保持最简。）
                out = c.expect();
            } else {
                // 真实 SUT：调用大模型，并捕获 token 用量用于预算守卫。
                // Spring AI 2.x：call() 返回 CallResponseSpec；.chatResponse() 得到 ChatResponse。
                ChatResponse resp = chatClient.prompt().user(c.input()).call().chatResponse();
                // ChatResponse(普通类)→getResult() 得 Generation；getOutput() 得 AssistantMessage(普通类)；
                // AssistantMessage 继承 AbstractMessage，取文本用 getText()（来自 Content 接口，非 content()/getContent()）。
                out = resp.getResult().getOutput().getText();
                // getMetadata()→ChatResponseMetadata；getUsage()→Usage（可空，故做空值守卫）。
                Usage usage = resp.getMetadata().getUsage();
                Integer total = usage != null ? usage.getTotalTokens() : null;
                if (total != null) {
                    used += total;
                }
            }

            boolean ok = scorer.score(out, c.expect());
            if (ok) pass++;
            OUT.printf("[%s] %s%n       期望含: %s%n       实际  : %s%n%n",
                    ok ? "PASS" : "FAIL", c.input(), c.expect(), out);

            // token 预算守卫：真实 eval 且设了上限，累计超了立即标红
            if (!mock && budget > 0 && used > budget) {
                fail(String.format("eval token 预算超限：已用 %d > 上限 %d（调高 -Deval.budget.tokens 或设 0 关闭）", used, budget));
            }
        }

        OUT.printf("===== eval 通过率 %d/%d（mock=%s, 已用 token=%d）=====%n", pass, cases.size(), mock, used);
        // eval 通过率门禁：全部用例必须过，否则标红
        assertEquals(pass, cases.size());
    }
}
