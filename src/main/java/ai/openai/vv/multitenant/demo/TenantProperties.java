package ai.openai.vv.multitenant.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 绑定 application.yaml 里的 tenants: 段（一个租户列表，不是 Map）。
 *
 * 重要（踩坑结论，已用真实 Spring 容器验证）：
 *   Spring Boot 4.1.0 的 @ConfigurationProperties 构造器绑定，
 *   无法把 Map<String, 对象>（值类型是 record / bean）绑上 —— 实测 size=null。
 *   但 List<扁平 record> 完全正常（实测 size=2）。
 *   所以这里用 List<TenantConfig>，由 TenantRepoRegistry 再按需拼成 Map<租户ID, 数据源>。
 *
 * 注意：这里【不写 prefix】，字段名 tenants 直接对应 YAML 顶层的 tenants: 列表。
 * 也不写 @Component —— 由主类的 @EnableConfigurationProperties(TenantProperties.class) 注册并绑定。
 */
@ConfigurationProperties
public record TenantProperties(List<TenantConfig> tenants) {

    /** 单个租户的连接信息（扁平 record，字段即 YAML 里的 key） */
    public record TenantConfig(String id, String url, String username, String password) {
    }
}
