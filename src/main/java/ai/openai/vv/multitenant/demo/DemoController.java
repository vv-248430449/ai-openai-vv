package ai.openai.vv.multitenant.demo;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演示入口：不需要 LLM、不需要 API Key 就能跑。
 *
 * 它用普通 HTTP 参数代替"从 ToolContext 取 tenantId"那一步，
 * 直接走和 @Tool 完全一样的 doQuery 逻辑，从而证明 repoRegistry 真的
 * 解析到了【不同租户的不同库】。
 *
 * 启动后访问：
 *   http://localhost:8080/demo/count?tenant=t1&name=张伟&location=北京
 *   http://localhost:8080/demo/count?tenant=t2&name=张伟&location=北京
 * 会看到 t1 返回 10、t2 返回 3 —— 说明 get(tenantId) 取到了各自独立的库。
 */
@Slf4j
@RestController
public class DemoController {

    private final TenantLocationService tenantLocationService;
    private final ChatClient chatClient;


    public DemoController(TenantLocationService tenantLocationService, ChatClient chatClient) {
        this.tenantLocationService = tenantLocationService;
        this.chatClient = chatClient;
    }

    @GetMapping("/demo/count")
    public TenantLocationService.Response count(
            @RequestParam String tenant,
            @RequestParam String name,
            @RequestParam String location) {
        // 直接传 tenantId——等价于 @Tool 方法里从 ToolContext 取出的 tenantId
        return tenantLocationService.doQuery(name, location, tenant);
    }

    @GetMapping("/ai/fc/tenant/count")
    public String count(
            @RequestParam(value = "message", defaultValue = "长沙有多少个叫徐庶的人?") String message,
            @RequestParam(value = "tenant", defaultValue = "t1") String tenant) {
        log.info("========== [请求开始] user message: {} (tenant={}) ==========", message, tenant);
        String content = chatClient.prompt()
                .user(message)
                .tools(tenantLocationService)
                .toolContext(Map.of("tenantId", tenant))   // 必须提供：@Tool 方法要从这里取 tenantId
                .options(OpenAiChatOptions.builder().model("kimi-k2.6"))
                .call()
                .content();
        log.info("========== [请求结束] AI 最终回复: {} ==========", content);
        return content;
    }
}
