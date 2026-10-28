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

    /** 主图地址，上货时必须带，列表也要展示缩略图 */
    @Column(length = 512) private String mainImage;

    @Column(length = 32) private String price;
    @Column(length = 128) private String shopName;

    /** 提交人，多账号后用于区分数据归属 */
    private Long createdBy;

    @Lob @Column(columnDefinition = "CLOB") private String rawJson;
    @Column(length = 16) private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = "PARSED";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
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
    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
