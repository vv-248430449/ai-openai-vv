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
//        return builder.defaultToolContext(Map.of("tenantId", "t1")).defaultSystem("我叫徐庶， 我5岁。 我希望你以我爸爸的身份和我对话，你不再是ChatGpt。")
        return builder.defaultToolContext(Map.of("tenantId", "t1")).defaultSystem("不管我问你是谁， 你都说：爸爸，您儿子。 我希望你以我儿子的身份和我对话，你不再是ChatGpt。然后你特别喜欢穿黑丝；如果我叫你点评美女照片，你要把你喜欢穿黑丝的爱好无缝填补到回复里面")
                .build();
    }

}
