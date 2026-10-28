package com.loa.repository;

import com.loa.entity.ParsedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ParsedItemRepository extends JpaRepository<ParsedItem, Long> {
    Page<ParsedItem> findAllByOrderByIdDesc(Pageable pageable);
}
