package com.loa.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parsed_items")
public class ParsedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sourceLinkId;

    @Column(length = 32) private String platform;
    @Column(length = 512) private String sourceUrl;
    @Column(length = 512) private String title;
    @Column(length = 32) private String price;
    @Column(length = 128) private String shopName;
    @Lob @Column(columnDefinition = "CLOB") private String rawJson;
    @Column(length = 16) private String status;
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "PARSED";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSourceLinkId() { return sourceLinkId; }
    public void setSourceLinkId(Long sourceLinkId) { this.sourceLinkId = sourceLinkId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
