package ai.openai.vv.multitenant.demo;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 一个租户的数据访问对象（DAO）。
 *
 * 它"绑定"在某个租户的 DataSource 上——构造时接收一个 JdbcTemplate，
 * 而 JdbcTemplate 又持有该租户专属的 DataSource（连接池）。
 *
 * 重点：TenantRepo 不是"一个共享单例"，而是【每个租户一个实例】，
 * 各自连着自己的库。
 */
public class TenantRepo {

    private final JdbcTemplate jdbc;

    public TenantRepo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 仅供演示：建表/灌种子时用 */
    public JdbcTemplate getJdbc() {
        return jdbc;
    }

    /** 统计某地区同名人数——SQL 直接打在该租户自己的库上 */
    public long countByNameAndLocation(String name, String location) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE name = ? AND location = ?",
                Long.class, name, location);
        return n == null ? 0L : n;
    }
}
