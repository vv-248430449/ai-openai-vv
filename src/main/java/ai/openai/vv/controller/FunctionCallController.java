package ai.openai.vv.controller;

import ai.openai.vv.callbacks.LocationNamesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FunctionCallController {

    private final ChatClient chatClient;
    private final LocationNamesService locationNamesService;

    /**
     * function-call 应对大模型无法获取实时信息的弊端
     * Spring AI 2.0 中 withFunction 已被移除，改为通过 ChatClient.tools() 注入 @Tool 注解的工具方法。
     *
     * @see <a href="https://docs.spring.io/spring-ai/reference/api/tools.html">Spring AI Tools Calling</a>
     */
    @GetMapping("/ai/fc")
    public String fc(@RequestParam(value = "message", defaultValue = "长沙有多少个叫徐庶的人?") String message) {
        log.info("========== [请求开始] user message: {} ==========", message);
        String content = chatClient.prompt()
                .user(message)
                .tools(locationNamesService)
                .options(OpenAiChatOptions.builder().model("kimi-k2.6"))
                .call()
                .content();
        log.info("========== [请求结束] AI 最终回复: {} ==========", content);
        return content;
    }
}
