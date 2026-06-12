package com.campus.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductListRequest {

    private String keyword;
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    @jakarta.validation.constraints.Pattern(regexp = "price_asc|price_desc|newest", message = "排序方式不合法")
    private String sort;
    private String conditionLevel;
}
