package ai.openai.vv;

/**
 * eval 三段式里的 Cases 组件：单条「输入 → 期望」用例。
 *
 * <p>对应 eval 三段式：
 *   - input  ：喂给 SUT（被测系统，这里是 ChatClient / Controller）的问题；
 *   - expect ：期望输出里包含的关键词（Lv1 子串匹配判定用）；
 *   - scorer ：用哪一级评分器（L1 规则 / L2 LLM-judge / L3 金标准），默认 L1。
 *
 * <p>放在 src/test/resources/eval-cases.json，加用例 = 加一行 JSON，不用改 Java。
 */
public record EvalCase(String input, String expect, String scorer) {

    /** 评分器级别，缺省按 L1（规则法、零成本）处理。 */
    public String scorer() {
        return scorer == null || scorer.isBlank() ? "L1" : scorer;
    }
}
