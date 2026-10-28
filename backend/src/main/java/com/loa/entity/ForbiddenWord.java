package com.loa.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "forbidden_words")
public class ForbiddenWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 64)
    private String word;

    @Column(length = 32)
    private String category;

    /** 1=违规词 2=敏感词 3=平台禁用词，数字越大越严重 */
    private Integer level;

    /** "1" 启用、"0" 停用。停用而不删除，避免误删后无法追溯 */
    @Column(length = 8)
    private String enabled;

    @Column(length = 255)
    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public ForbiddenWord() {}

    public ForbiddenWord(String word, String category, Integer level) {
        this.word = word;
        this.category = category;
        this.level = level;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (enabled == null) enabled = "1";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
