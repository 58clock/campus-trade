package com.campus.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class StatisticsVO {

    private Long totalProducts;
    private Long totalOrders;
    private Long totalUsers;
    private BigDecimal totalSales;
    private List<DailySales> weeklySales;
    private Map<String, Long> categoryDistribution;
    private Map<String, Long> orderStatusDistribution;

    @Data
    public static class DailySales {
        private String day;
        private BigDecimal total;
    }
}
