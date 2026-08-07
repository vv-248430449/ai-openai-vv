package ai.openai.vv;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * Lv2 · LLM-as-judge 评分器（Scorer）。
 *
 * <p>用另一个大模型调用当"评分员"，判断 SUT 输出是否满足 expect。
 * 此时 expect 不再是关键词，而是<b>评分要点 / 标准答案描述</b>——专门兜 Lv1 子串匹配
 * 无能为力的<b>开放题</b>（解释类、论述类、多要点题）。
 *
 * <p>设计要点：
 *   - 评分员用 system prompt 约束为"只答 YES/NO 的严格评分员"，降低 judge 噪声；
 *   - 解析策略：判 verdict 以 YES 开头即达标。模型若跑偏（答了长篇），startsWith 仍能兜住；
 *   - 与 SUT 共用同一 ChatClient（同一模型）。生产环境可注入<b>不同/更强</b>的 judge 模型提升判别力，
 *     此处为演示最小可用，复用现有配置。
 *   - judge 调用本身也可能失败（无 key / 联网异常），按"宁可红不假绿"原则，异常会直接冒泡使测试标红。
 */
public class LlmJudgeScorer implements Scorer {

    private final ChatClient judge;

    /** @param judge 充当评分员的 ChatClient（通常复用 SUT 同实例或独立的 judge 实例） */
    public LlmJudgeScorer(ChatClient judge) {
        this.judge = judge;
    }

    @Override
    public boolean score(String output, String rubric) {

        if (output == null || output.isBlank()) return false;

        ChatResponse resp = judge.prompt()
                .system("你是一个严格、客观的评分员。只依据下面给出的「评分要点」判断回答是否合格，"
                        + "不要被回答的流畅度或长度干扰。只回复 YES 或 NO，不要任何解释。")
                .user("【评分要点】\n" + rubric
                        + "\n\n【待评分回答】\n" + output
                        + "\n\n该回答是否满足上述评分要点？只答 YES 或 NO。")
                .call()
                .chatResponse();

        String verdict = resp.getResult().getOutput().getText();
        return verdict != null && verdict.trim().toUpperCase().startsWith("YES");
    }
}
