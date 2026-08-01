package ai.openai.vv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ai.openai.vv.multitenant.demo.TenantProperties;

@SpringBootApplication
@EnableConfigurationProperties(TenantProperties.class)
public class AiOpenaiVvApplication {

    public static void main(String[] args) {
        // 设置代理，这里可以网上找一些免费代理，或者收费代理
//        String proxy = "127.0.0.1";  // 100.100.101.235 8811  示例，里面填具体的代理ip
//        int port = 7897;   //代理的端口，
//        System.setProperty("proxyType", "4");
//        System.setProperty("proxyPort", Integer.toString(port));
//        System.setProperty("proxyHost", proxy);
//        System.setProperty("proxySet", "true");

        SpringApplication.run(AiOpenaiVvApplication.class, args);
    }

}
