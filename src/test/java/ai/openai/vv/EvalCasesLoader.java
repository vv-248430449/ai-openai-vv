package ai.openai.vv;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

/**
 * eval 三段式里的 Cases 加载器：从 classpath 读取 eval-cases.json，
 * 反序列化为 {@link EvalCase} 列表。不依赖 Spring，纯 Java，可被任意测试复用。
 *
 * <p>用例数据化后，加一条 eval 用例 = 在 JSON 里加一行，而不是改 Java 代码——
 * Oracle（判定器）变成"会增长的数据集"，eval 从"写死的脚本"升级为"可维护的资产"。
 */
public final class EvalCasesLoader {

    private EvalCasesLoader() {
    }

    public static List<EvalCase> load(String classPath) throws Exception {
        ObjectMapper om = new ObjectMapper();
        try (InputStream is = EvalCasesLoader.class.getClassLoader().getResourceAsStream(classPath)) {
            if (is == null) {
                throw new IllegalStateException("找不到 eval cases 文件: " + classPath
                        + "（应在 src/test/resources/ 下）");
            }
            return om.readValue(is, new TypeReference<List<EvalCase>>() {
            });
        }
    }
}
