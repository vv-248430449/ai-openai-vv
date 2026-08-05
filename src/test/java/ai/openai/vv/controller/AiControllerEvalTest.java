package ai.openai.vv.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * ② 覆盖率门禁的"驱动样本"：用 MockMvc 在 HTTP 层驱动 Controller，
 * 验证 {@code /ai/simple} 把用户消息交给 ChatClient、把模型回答包进 JSON 的契约。
 *
 * <p>Spring Boot 4.x 已移除 {@code @AutoConfigureMockMvc} 与 {@code @MockBean}，
 * 故改用 {@code @TestConfiguration} 提供一个 {@code @Primary} 的 Mockito 深桩 ChatClient，
 * 再用 {@code MockMvcBuilders.webAppContextSetup} 手动构建 MockMvc（均在 spring-test 内）。
 *
 * <p>这里故意替掉真实大模型：
 *  - 确定性：每次结果固定，不依赖网络/LLM 抽风；
 *  - 零成本：不耗 token、无需 OPENAI_KEY；
 *  - 可重复：离线环境也能跑，覆盖率门禁不会因外部依赖变红。
 *
 * <p>真·模型"输出正确性"由 {@code AiEvalTest}（@Tag("eval")）在 CI 用真实 Key 跑，
 * 两者分工：本测试管"Controller 接线对不对"，AiEvalTest 管"模型答得对不对"。
 */
@SpringBootTest
class AiControllerEvalTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void evalSimpleEndpointReturnsModelAnswer() throws Exception {
        // 手动构建 MockMvc（SB4 无 @AutoConfigureMockMvc，改用 webAppContextSetup）
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // 通过 HTTP 真实驱动 Controller，断言响应里包含模型回答 "2"
        String resp = mockMvc.perform(get("/ai/simple")
                        .param("message", "1+1 等于几？只回答数字。"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(resp.contains("2"), "接口回答未通过 eval: " + resp);
    }

    /** 用深桩 Mockito 替掉 ChatClient，使 prompt().user(..).call().content() 固定返回 "2"。 */
    @TestConfiguration(proxyBeanMethods = false)
    static class MockChatClientConfig {
        @Bean
        @Primary
        ChatClient chatClientMock() {
            ChatClient mock = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
            when(mock.prompt().user(anyString()).call().content()).thenReturn("2");
            return mock;
        }
    }
}
