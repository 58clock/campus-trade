package com.campus.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewVO {

    private Long id;
    private Long orderId;
    private Long reviewerId;
    private String reviewerName;
    private String reviewerAvatar;
    private Integer rating;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
