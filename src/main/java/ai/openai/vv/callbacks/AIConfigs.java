package ai.openai.vv.callbacks;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfigs {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultSystem("我叫徐庶， 我5岁。 我希望你以我爸爸的身份和我对话，你不再是ChatGpt。")
                .build();
    }

}
