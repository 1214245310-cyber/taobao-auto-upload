package com.loa.entity;

import jakarta.persistence.*;

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

    private Integer level;

    public ForbiddenWord() {}
    public ForbiddenWord(String word, String category, Integer level) {
        this.word = word;
        this.category = category;
        this.level = level;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
}
