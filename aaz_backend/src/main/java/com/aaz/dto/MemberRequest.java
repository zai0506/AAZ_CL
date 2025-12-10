package com.aaz.dto;

import lombok.Data;

@Data
public class MemberRequest {
    private Long id;  // 如果有 id 代表要更新現有成員，沒有代表新增成員
    private Long userId;
    private String displayName;
}