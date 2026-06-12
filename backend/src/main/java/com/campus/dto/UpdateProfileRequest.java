package com.campus.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String nickname;
    private String school;
    private String phone;
    private String email;
}
