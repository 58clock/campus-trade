package com.campus.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String nickname;
    private String school;
    private String avatar;
    private String role;
    private Integer status;
    private Double reputationScore;
    private LocalDateTime createdAt;
}
