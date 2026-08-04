package ai.openai.vv;

import java.util.Map;

/**
 * Lv1 规则法评分器（Scorer）——纯逻辑组件，不依赖 Spring、不调用大模型，可独立单测。
 *
 * 设计要点（对应 eval 三段式里的 Scorer）：
 *   把「模型输出 output」与「期望关键词 expect」都归一化后做 contains 判定。
 *   归一化会把中文数字（一二三…/两）映射成阿拉伯数字，从而修复
 *   「模型答『二』但 expect 是『2』就误判 FAIL」的 bug。
 *
 * 已知 Lv1 局限：contains 语义对数字题仍可能误判（如答『22』会误 PASS），
 *   进阶可换 Lv2 LLM-as-judge 或精确匹配，用到再说，不一步到位。
 */
public class RuleScorer {

    private static final Map<String, String> CN_TO_DIGIT = Map.ofEntries(
            Map.entry("零", "0"), Map.entry("一", "1"), Map.entry("二", "2"),
            Map.entry("两", "2"), Map.entry("三", "3"), Map.entry("四", "4"),
            Map.entry("五", "5"), Map.entry("六", "6"), Map.entry("七", "7"),
            Map.entry("八", "8"), Map.entry("九", "9")
    );

    /** 归一化：中文数字 → 阿拉伯数字，空白不影响比对 */
    private static String normalize(String s) {
        if (s == null) return "";
        String r = s;
        for (Map.Entry<String, String> e : CN_TO_DIGIT.entrySet()) {
            r = r.replace(e.getKey(), e.getValue());
        }
        return r;
    }

    /**
     * 评分：输出里包含期望（归一化后）即算通过。
     * 入参为空或空白直接返回 false（null 安全）。
     */
    public boolean score(String output, String expect) {
        if (output == null || expect == null) return false;
        if (output.isBlank() || expect.isBlank()) return false;
        return normalize(output).contains(normalize(expect));
    }
}
