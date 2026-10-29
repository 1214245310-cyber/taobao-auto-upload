package com.loa.repository;

import com.loa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    /**
     * 邮箱按 id 升序取：正常情况下库里邮箱唯一（写入前查重 + 唯一索引），
     * 但万一历史数据出现重复，也能拿到全部候选，由调用方给出明确提示，
     * 而不是让 Spring Data 抛 IncorrectResultSizeDataAccessException 变成 500。
     */
    List<User> findByEmailOrderByIdAsc(String email);

    boolean existsByEmail(String email);
}
