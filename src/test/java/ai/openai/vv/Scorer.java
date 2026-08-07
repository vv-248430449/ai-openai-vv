package ai.openai.vv;

/**
 * eval 三段式里的 Scorer（评分器）统一接口。
 *
 * <p>实现：
 *   - {@link RuleScorer}   ：Lv1 规则法（中文数字归一化 + 子串匹配），零成本、不调模型；
 *   - {@link LlmJudgeScorer}：Lv2 LLM-as-judge（用另一个模型调用当评分员），兜开放题。
 *
 * <p>{@link AiEvalTest} 按用例 {@code scorer} 字段分流到具体实现，
 * 未知级别直接抛异常——这保证 scorer 字段是被真实消费的，而非装饰字段。
 */
public interface Scorer {

    /**
     * 评分：判断 SUT 输出是否达标。
     *
     * @param output 被测系统的实际输出
     * @param expect 期望（Lv1 为关键词；Lv2 为评分要点/标准答案）
     * @return true 表示达标
     */
    boolean score(String output, String expect);
}
