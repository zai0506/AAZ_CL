package com.aaz.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String nickname;
    private String email;
    private String oldPassword;
    private String newPassword;
}
