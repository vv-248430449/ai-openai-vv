package ai.openai.vv;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RuleScorer 的纯逻辑单测（@Tag("unit")）。
 * 不启动 Spring、不调用大模型、零成本，可高频跑。
 */
@Tag("unit")
class EvalScorerTest {

    private final RuleScorer scorer = new RuleScorer();

    @Test
    void digitExpectMatchesArabicOutput() {
        assertTrue(scorer.score("答案是2", "2"));
    }

    @Test
    void digitExpectMatchesChineseOutput() { // 修复前的 bug：模型答『二』会被判 FAIL
        assertTrue(scorer.score("答案是二", "2"));
    }

    @Test
    void digitExpectMatchesLiangOutput() {
        assertTrue(scorer.score("答案是两", "2"));
    }

    @Test
    void freeformContainsStillWorks() {
        assertTrue(scorer.score("北京是中国的首都", "北京"));
    }

    @Test
    void nullOutputFails() {
        assertFalse(scorer.score(null, "2"));
    }

    @Test
    void blankOutputFails() {
        assertFalse(scorer.score("   ", "2"));
    }

    @Test
    void nullExpectFails() {
        assertFalse(scorer.score("答案是2", null));
    }

    @Test
    void mismatchFails() {
        assertFalse(scorer.score("答案是3", "2"));
    }
}
