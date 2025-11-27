package com.aaz.dto;

import lombok.Data;

@Data
public class MemberRequest {
    private Long userId;
    private String displayName;
}