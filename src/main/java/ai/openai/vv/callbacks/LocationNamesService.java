package ai.openai.vv.callbacks;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Spring AI 2.0 中，Function Bean 模式已被移除，改为使用 @Tool 注解声明工具方法。
 * 工具方法可直接使用原始 POJO 作为参数，不再需要实现 Function 接口。
 */
@Slf4j
@Service
public class LocationNamesService {

    @Tool(description = "根据姓名和地区，查询该地区有多少个同名的人。当用户询问某个地区有多少人叫某个名字时，必须调用此工具。")
    public Response getLocationAndNum(
            @ToolParam(description = "要查询的姓名") String name,
            @ToolParam(description = "要查询的地区，如城市名") String location) {
        log.info("========== [Tool 第1轮] 工具被调用：name={}, location={} ==========", name, location);
        if (location == null || name == null) {
            log.warn("[Tool] 参数缺失，返回默认值");
            return new Response("参数缺失，无需function-call，正常响应即可..");
        }
        Response response = new Response(location + "有10个叫" + name + "的人！");
        log.info("========== [Tool 第1轮] 工具返回结果：{} ==========", response.message());
        return response;
    }

    public record Response(String message) {}

}
