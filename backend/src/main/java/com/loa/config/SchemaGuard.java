package com.loa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时补齐 users 表的唯一索引。
 *
 * 为什么需要：邮箱既是登录凭据也是找回密码凭据，用户名是登录凭据。
 * 代码里注册 / 新增 / 换绑都会先查重，但"查重"与"写入"之间存在并发窗口，
 * 两个人同时提交同一个邮箱或用户名仍可能都写进去 —— 一旦重复，
 * findByEmail / findByUsername 查到两条会抛异常，登录直接 500。
 * 实体上的 unique=true 只在首次建表时生效，已有表不会被 Hibernate 补上，所以这里兜底。
 *
 * 做法不硬来：若历史数据已有重复，只打告警、不建索引，避免因为一条索引把服务拉挂。
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
        ensureEmailUnique();
        ensureUsernameUnique();
    }

    private void ensureEmailUnique() {
        ensureUnique("uk_users_email", "email",
                "select count(*) from (select email from users where email is not null and email <> '' "
                        + "group by email having count(*) > 1) t",
                "空值不受限制", "解绑或改掉重复邮箱");
    }

    private void ensureUsernameUnique() {
        ensureUnique("uk_users_username", "username",
                "select count(*) from (select username from users where username is not null and username <> '' "
                        + "group by username having count(*) > 1) t",
                "注册与新增用户都必须唯一", "改名或删除重复账号");
    }

    /**
     * @param indexName 索引名
     * @param column    加索引的列
     * @param dupSql    统计重复值的 SQL（返回重复组数）
     * @param okNote    建索引成功时追加到日志的说明
     * @param fixHint   出现重复时的处理建议
     */
    private void ensureUnique(String indexName, String column, String dupSql,
                              String okNote, String fixHint) {
        try {
            Integer dup = jdbc.queryForObject(dupSql, Integer.class);
            if (dup != null && dup > 0) {
                log.warn("users.{} 存在 {} 组重复值，暂不创建唯一索引；"
                        + "请在「用户管理」里{}后重启服务", column, dup, fixHint);
                return;
            }
            jdbc.execute("create unique index if not exists " + indexName
                    + " on users(" + column + ")");
            log.info("users.{} 唯一索引已就绪（{}）", column, okNote);
        } catch (Exception e) {
            log.warn("创建 users.{} 唯一索引失败，不影响启动：{}", column, e.getMessage());
        }
    }
}
