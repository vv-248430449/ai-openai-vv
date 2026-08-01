package ai.openai.vv.callbacks;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class AIConfigs {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        //单租户，全局默认
        return builder.defaultToolContext(Map.of("tenantId", "t1")).defaultSystem("我叫徐庶， 我5岁。 我希望你以我爸爸的身份和我对话，你不再是ChatGpt。")
                .build();
    }

}
