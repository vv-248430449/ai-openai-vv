package ai.openai.vv.multitenant.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * 把"按租户查同名人数"做成一个 Spring AI 2.0 的 @Tool。
 *
 * 关键点：tenantId 不是构造器注入（那会把 repo 钉死成单例），
 * 而是【每次调用时】从 ToolContext 里取——这正是上一轮聊的
 * "上下文跟着每次调用走"的实处。
 */
@Slf4j
@Service
public class TenantLocationService {

    private final TenantRepoRegistry repoRegistry;

    public TenantLocationService(TenantRepoRegistry repoRegistry) {
        this.repoRegistry = repoRegistry;
    }

    @Tool(description = "根据姓名和地区，查询【当前租户】该地区有多少个同名的人")
    public Response getLocationAndNum(
            @ToolParam(description = "要查询的姓名") String name,
            @ToolParam(description = "要查询的地区，如城市名") String location,
            ToolContext ctx) {                        // ← 每次调用 Spring AI 自动注入

        // 从"随调用走"的上下文里取租户ID（由调用方在 ChatClient 上 set 进去）
        // ToolContext.getContext() 返回整张 Map，再 .get(key) 取值
        String tenantId = (String) ctx.getContext().get("tenantId");
        log.info("[Tool] 本次调用 tenantId={}, name={}, location={}", tenantId, name, location);

        return doQuery(name, location, tenantId);
    }

    /**
     * 真正的查询逻辑。抽出来，让 DemoController 不走 LLM 也能复用同一份逻辑。
     * 这一步就是 repoRegistry 的用武之地：
     *   repoRegistry.get(tenantId)  ->  该租户专属的 TenantRepo  ->  查它自己的库
     */
    public Response doQuery(String name, String location, String tenantId) {
        long n = repoRegistry.get(tenantId).countByNameAndLocation(name, location);
        return new Response("租户[" + tenantId + "] 的 " + location + " 有 " + n + " 个叫 " + name + " 的人");
    }

    public record Response(String message) {
    }

    /*
     * ── 调用方（Controller / Advisor）如何把 tenantId 送进 ToolContext ──
     * 这一步不需要在 @Tool 方法里写，而是在发起 ChatClient 调用时设置。
     * 注意：ChatClient.toolContext(...) 接收的是 Map<String,Object>（不是 ToolContext 对象），
     * Spring AI 会在调用工具时把这张 Map 包成 ToolContext 注入给方法参数。
     *
     *   chatClient.prompt()
     *           .user(message)
     *           .tools(tenantLocationService)
     *           .toolContext(Map.of("tenantId", currentTenant))  // currentTenant 来自 JWT / 会话
     *           .call()
     *           .content();
     *
     * 这样 tenantId 跟着"这次对话/这次工具调用"走，
     * 换线程、甚至走 MCP 跨进程调用都不会丢（不像 ThreadLocal）。
     */
}
