package com.loa.repository;

import com.loa.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserTokenRepository extends JpaRepository<UserToken, String> {

    List<UserToken> findByUserId(Long userId);

    List<UserToken> findByExpiresAtBefore(LocalDateTime time);
}
