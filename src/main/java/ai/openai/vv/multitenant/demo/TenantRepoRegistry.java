package ai.openai.vv.multitenant.demo;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ============================================================================
 * repoRegistry —— 多租户数据源解析器的"心脏"
 * ============================================================================
 *
 * 它到底是什么？
 *   一个普通的 @Component（它自身是单例），内部只持有一张：
 *        Map<租户ID, 该租户的 TenantRepo>
 *   仅此而已，没有魔法。
 *
 * 它怎么"来"的（生命周期）？
 *   1) Spring 把 application.yaml 的 tenants: 段绑定进 TenantProperties；
 *   2) 本类构造器拿到 TenantProperties；
 *   3) @PostConstruct 里遍历每个租户：
 *        - 用它的 url/user/pass 建一个 HikariDataSource
 *          （= 一个独立的连接池 = 一个独立的库）
 *        - 包成 JdbcTemplate -> 包成 TenantRepo
 *        - 以租户ID为 key 塞进 Map
 *   4) 之后 get(tenantId) 只是一次 Map.get，微秒级，不再建连接。
 *
 * 所谓"按租户现场解析数据源"，剥掉外壳就是：一次 Map 查找。
 * ============================================================================
 */
@Component
public class TenantRepoRegistry {

    private final TenantProperties properties;
    private final Map<String, TenantRepo> repos = new LinkedHashMap<>();

    public TenantRepoRegistry(TenantProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        for (TenantProperties.TenantConfig cfg : properties.tenants()) {
            String tenantId = cfg.id();   // YAML 里每个租户显式写的 id，作为 Map 的 key
            // 1) 每租户一个独立 DataSource（独立连接池）
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(cfg.url());
            ds.setUsername(cfg.username());
            ds.setPassword(cfg.password());
            ds.setPoolName("hikari-" + tenantId);

            // 2) 包成 JdbcTemplate -> TenantRepo
            TenantRepo repo = new TenantRepo(new JdbcTemplate(ds));

            // 3) 演示用：建表 + 灌种子数据（真实项目应交给 Flyway/Liquibase migration）
            repo.getJdbc().execute(
                    "CREATE TABLE IF NOT EXISTS person (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), location VARCHAR(100))");
            seedIfEmpty(repo, tenantId);

            // 4) 以租户ID为 key 落进 Map
            repos.put(tenantId, repo);
            System.out.println("[repoRegistry] 已注册租户数据源: " + tenantId + " -> " + cfg.url());
        }
    }

    /**
     * 解析：给定租户ID，返回它专属的 repo。
     * 这就是"现场解析"的唯一动作——一次 Map.get。
     */
    public TenantRepo get(String tenantId) {
        TenantRepo repo = repos.get(tenantId);
        if (repo == null) {
            throw new IllegalArgumentException("未知租户: " + tenantId + "，已注册: " + repos.keySet());
        }
        return repo;
    }

    /** 仅演示：按租户给不同初始数据，方便一眼看出"解析到了不同库" */
    private void seedIfEmpty(TenantRepo repo, String tenantId) {
        Long existing = repo.getJdbc().queryForObject("SELECT COUNT(*) FROM person", Long.class);
        if (existing != null && existing > 0) {
            return;
        }
        int count = "t1".equals(tenantId) ? 10 : 3;   // t1 灌10条，t2 灌3条
        for (int i = 0; i < count; i++) {
            repo.getJdbc().update("INSERT INTO person(name, location) VALUES (?, ?)", "张伟", "北京");
        }
    }
}
