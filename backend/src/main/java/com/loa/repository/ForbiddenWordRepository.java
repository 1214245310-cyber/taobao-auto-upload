package com.loa.repository;

import com.loa.entity.ForbiddenWord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ForbiddenWordRepository extends JpaRepository<ForbiddenWord, Long> {
    Optional<ForbiddenWord> findByWord(String word);
}
