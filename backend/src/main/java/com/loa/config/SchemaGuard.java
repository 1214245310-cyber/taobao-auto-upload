package com.loa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 给 users.email 补一个唯一索引。
 *
 * 原因：邮箱既是登录凭据，也是找回密码凭据。代码里注册 / 新增 / 换绑都先查重，
 * 但"查重"和"写入"之间存在并发窗口，两个人同时提交同一邮箱仍可能都写进去；
 * 一旦出现两个账号共用一个邮箱，邮箱登录就无法判断该进哪个号。
 *
 * 这里做兜底而不是硬性 DDL：如果历史数据已经有重复（或存在多条空串），
 * 建索引会失败，只打告警、不影响启动，避免因为一条索引把整个服务拉挂。
 */
@Component
public class SchemaGuard implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaGuard.class);

    private final JdbcTemplate jdbc;

    public SchemaGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer dup = jdbc.queryForObject(
                    "select count(*) from (select email from users where email is not null and email <> '' "
                            + "group by email having count(*) > 1) t",
                    Integer.class);
            if (dup != null && dup > 0) {
                log.warn("users 表存在 {} 个被多个账号占用的邮箱，暂不创建唯一索引；"
                        + "请在「用户管理」里解绑或改掉重复邮箱后重启服务", dup);
                return;
            }
            jdbc.execute("create unique index if not exists uk_users_email on users(email)");
            log.info("users.email 唯一索引已就绪（空值不受限制）");
        } catch (Exception e) {
            log.warn("创建 users.email 唯一索引失败，不影响启动：{}", e.getMessage());
        }
    }
}
