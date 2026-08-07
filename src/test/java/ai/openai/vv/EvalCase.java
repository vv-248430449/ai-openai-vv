package ai.openai.vv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * eval 三段式里的 Cases 组件：单条「输入 → 期望」用例。
 *
 * <p>对应 eval 三段式：
 *   - input  ：喂给 SUT（被测系统，这里是 ChatClient / Controller）的问题；
 *   - expect ：期望输出里包含的关键词（Lv1 子串匹配判定用）；
 *   - scorer ：用哪一级评分器（L1 规则 / L2 LLM-judge / L3 金标准），默认 L1；
 *   - mockOutput ：【仅离线 mock 模式用】手工声明一个"被 mock 掉的模型输出"，
 *                  用来替代真实模型返回，让 mock 模式也能真验一次打分逻辑。
 *
 * <p>术语对照（本任务踩出来的新词，详见知识库 wiki/programming/eval-three-stage.md）：
 *   - 烟囱测试（smoke test）/ 管线装配测试（pipeline-plumbing test）：
 *        只验证"整条管线能不能跑通、各部件有没有接错"，不验证业务正确性。
 *        名字来自硬件：通电冒烟就知道没短路。AiEvalTest 的 mock 模式就是这类——
 *        它不调模型，只确认「JSON 加载→循环→打分→断言→报告」这条链路没断。
 *   - 对抗输入（adversarial input）：故意构造的、带噪声/陷阱的输入
 *        （如「你好，世界」带全角逗号、「答案是二」用中文数字），用来考验评分器
 *        是否足够鲁棒。EvalScorerTest 与 mockOutput 都靠它来验 scorer。
 *
 * <p>放在 src/test/resources/eval-cases.json，加用例 = 加一行 JSON，不用改 Java。
 * 注意：项目 pom 未开 -parameters，故用 @JsonCreator + @JsonProperty 显式绑定，
 * 并允许 mockOutput 在 JSON 中缺省（反序列化为 null，向后兼容）。
 */
public class EvalCase {

    private final String input;
    private final String expect;
    private final String scorer;
    private final String mockOutput;

    @JsonCreator
    public EvalCase(
            @JsonProperty("input") String input,
            @JsonProperty("expect") String expect,
            @JsonProperty("scorer") String scorer,
            @JsonProperty("mockOutput") String mockOutput) {
        this.input = input;
        this.expect = expect;
        this.scorer = scorer;
        this.mockOutput = mockOutput;
    }

    public String input() { return input; }
    public String expect() { return expect; }

    /** 评分器级别，缺省按 L1（规则法、零成本）处理。 */
    public String scorer() { return scorer == null || scorer.isBlank() ? "L1" : scorer; }

    /**
     * 离线 mock 模式的「假输出」。
     * 为 null 时 mock 退化回旧行为（out = expect，仅验链路），
     * 非空时作为对抗输入注入打分器，真验一次归一化/鲁棒性。
     */
    public String mockOutput() { return mockOutput; }
}
