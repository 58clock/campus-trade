package com.campus.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductVO {

    private Long id;
    private Long userId;
    private String sellerName;
    private String sellerAvatar;
    private String title;
    private String description;
    private String category;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String conditionLevel;
    private List<String> images;
    private String status;
    private Integer viewCount;
    private LocalDateTime createdAt;
}
