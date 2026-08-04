package ai.openai.vv;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ──────────────────────────────────────────────────────────────
 *  最小可用 eval（评估）演示 —— 直接迁移进 ai-openai-vv
 * ──────────────────────────────────────────────────────────────
 *
 * 为什么需要 eval？（对应「AI coding」新闻③）
 *   Agent / 大模型能把代码或回答生成得飞快，但「它写的对不对」没人敢直接信。
 *   eval = 用一组「输入 → 期望」用例，自动检查 AI 输出到底对不对、好不好。
 *
 * eval 永远逃不出三段式：
 *   1) SUT（被测系统，System Under Test）：本项目里就是 Spring 注入的 ChatClient。
 *   2) Scorer（评分器）：见 RuleScorer（已抽成独立纯逻辑组件，可单测）。
 *   3) Cases（用例集）：一组「问题 + 期望关键词」。
 *
 * 测试分层（本仓库约定）：
 *   - @Tag("unit")  ：纯逻辑单测（EvalScorerTest），不调模型、零成本，
 *                     本地 `mvn test` 默认跑（pom 用 excludedGroups 排除 eval）。
 *   - @Tag("eval")   ：本类，真实调用大模型做评估，默认被 pom 的 excludedGroups 排除，
 *                     仅 CI（带 key）或 scripts/test-eval.bat 显式开启时运行。
 *
 * 跑法：
 *   本地纯单测（免费）：   mvn test                                       （或 scripts\test.bat）
 *   真·模型 eval（花钱）： mvn test -Dtest=AiEvalTest -Dskip.eval.group=   （或 scripts\test-eval.bat）
 *   CI：                  mvn -B test -Dskip.eval.group=                   （跑全部，含 eval）
 *
 * 需要（必须）：application.yaml 配置好的【可用】API Key
 *   （本地环境变量 OPENAI_KEY，或 CI 的 secrets.OPENAI_KEY）+ 运行环境能联网。
 *   未配置密钥时真实调用会失败 → 测试标红（宁可红，也不要假绿）。
 * ──────────────────────────────────────────────────────────────
 */
@SpringBootTest
@Tag("eval")
class AiEvalTest {

    /** SUT：Spring 注入的真实 ChatClient（底层指向 application.yaml 配置的模型） */
    @Autowired
    ChatClient chatClient;

    /** Scorer（Lv1 规则法）：已抽成独立组件 RuleScorer，见 EvalScorerTest 的纯逻辑单测 */
    private final RuleScorer scorer = new RuleScorer();

    @Test
    void evalLlmAnswers() {
        // 必须配置可用的 OPENAI_KEY（本地环境变量 或 CI 的 secrets.OPENAI_KEY），
        // 否则下方真实调用大模型会失败 → 测试标红（已移除跳过逻辑，宁可红也不要假绿）。

        // Cases：问题 + 期望关键词（答案确定型任务最适合规则法）
        Object[][] cases = {
            {"1+1 等于几？只回答数字。", "2"},
            {"中国的首都是哪里？只回答城市名。", "北京"},
            {"水在标准常温下是哪种状态？从 固态/液态/气态 中选一个回答。", "液态"}
        };

        int pass = 0;
        for (Object[] c : cases) {
            String q = (String) c[0];
            String expect = (String) c[1];

            // SUT：真实调用大模型（这里就是 /ai/simple 背后的同一行代码）
            String out = chatClient.prompt().user(q).call().content();

            boolean ok = scorer.score(out, expect);
            if (ok) pass++;
            System.out.printf("[%s] %s%n       期望含: %s%n       实际  : %s%n%n",
                    ok ? "PASS" : "FAIL", q, expect, out);
        }
        System.out.printf("===== eval 通过率 %d/%d =====%n", pass, cases.length);
        // eval 通过率门禁：3 个用例必须全过，否则标红
        assertEquals(pass, cases.length);
    }
}
