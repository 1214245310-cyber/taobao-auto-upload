package com.loa.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 新增违禁词请求 */
public class AddForbiddenWordRequest {

    @NotBlank(message = "违禁词不能为空")
    @Size(max = 64, message = "违禁词最长 64 个字符")
    private String word;

    @Size(max = 32, message = "分类最长 32 个字符")
    private String category;

    /** 1=违规词 2=敏感词 3=平台禁用词，数字越大越严重 */
    @Min(value = 1, message = "等级取值范围 1-3")
    @Max(value = 3, message = "等级取值范围 1-3")
    private Integer level;

    @Size(max = 255, message = "备注最长 255 个字符")
    private String remark;

    /** "1" 启用、"0" 停用；为空默认启用 */
    @Size(max = 8)
    private String enabled;

    /** 匹配方式：等于 / 包含；为空默认「等于」 */
    @Size(max = 8)
    private String matchType;

    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }

    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
