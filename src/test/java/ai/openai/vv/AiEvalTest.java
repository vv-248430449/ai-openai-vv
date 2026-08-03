package ai.openai.vv;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
 *   2) Scorer（评分器）：把「输出」判定成 通过 / 不通过。最简 = 规则法 contains。
 *   3) Cases（用例集）：一组「问题 + 期望关键词」。
 *
 * 跑法：
 *   ./mvnw test -Dtest=AiEvalTest
 * 需要：
 *   application.yaml 里配置好的【可用】API Key + 运行环境能联网访问模型端点。
 *
 * 进阶（用到再说，别一步到位）：
 *   Lv2 LLM-as-judge（再调一次模型当裁判打分）
 *   Lv3 人工标注对照（攒标准答案做「模型 vs 人」一致性）
 * ──────────────────────────────────────────────────────────────
 */
@SpringBootTest
class AiEvalTest {

    /** SUT：Spring 注入的真实 ChatClient（底层指向 application.yaml 配置的模型） */
    @Autowired
    ChatClient chatClient;

    /** Scorer（Lv1 规则法）：输出里包含期望关键词就算通过 */
    boolean score(String output, String expect) {
        return output != null && output.contains(expect);
    }

    @Test
    void evalLlmAnswers() {
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

            boolean ok = score(out, expect);
            if (ok) pass++;
            System.out.printf("[%s] %s%n       期望含: %s%n       实际  : %s%n%n",
                    ok ? "PASS" : "FAIL", q, expect, out);
        }
        System.out.printf("===== eval 通过率 %d/%d =====%n", pass, cases.length);
    }
}
