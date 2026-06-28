package com.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.Result;
import com.campus.entity.BrowseHistory;
import com.campus.entity.Product;
import com.campus.mapper.BrowseHistoryMapper;
import com.campus.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

    private final ProductMapper productMapper;
    private final BrowseHistoryMapper browseHistoryMapper;
    private final JdbcTemplate jdbcTemplate;

    public Result<Map<String, Object>> recommend(Long userId, int limit) {
        // 确保表存在
        ensureTable();

        // 1. 查浏览历史（MP）
        List<BrowseHistory> history;
        try {
            history = browseHistoryMapper.selectList(
                    new LambdaQueryWrapper<BrowseHistory>()
                            .eq(BrowseHistory::getUserId, userId)
                            .orderByDesc(BrowseHistory::getCreatedAt)
                            .last("LIMIT 100"));
        } catch (Exception e) {
            log.warn("Query browse_history failed: {}", e.getMessage());
            history = List.of();
        }

        // 2. 统计分类频次和已浏览商品
        Map<String, Long> categoryCount = history.stream()
                .collect(Collectors.groupingBy(BrowseHistory::getCategory, LinkedHashMap::new, Collectors.counting()));
        Set<Long> viewedIds = history.stream()
                .map(BrowseHistory::getProductId)
                .collect(Collectors.toSet());

        // 3. 版块一：猜你喜欢 — 基于浏览分类
        List<Map<String, Object>> personalized = new ArrayList<>();
        if (!history.isEmpty()) {
            Set<Long> seen = new HashSet<>();
            List<Product> candidates = new ArrayList<>();
            for (String category : categoryCount.keySet()) {
                List<Product> catProducts = productMapper.selectList(
                        new LambdaQueryWrapper<Product>()
                                .eq(Product::getCategory, category)
                                .eq(Product::getStatus, "ON_SALE")
                                .ne(Product::getUserId, userId)
                                .orderByDesc(Product::getViewCount)
                                .last("LIMIT 15"));
                for (Product p : catProducts) {
                    if (seen.add(p.getId()) && !viewedIds.contains(p.getId())) {
                        candidates.add(p);
                    }
                }
            }
            candidates.sort((a, b) -> {
                long aCnt = categoryCount.getOrDefault(a.getCategory(), 0L);
                long bCnt = categoryCount.getOrDefault(b.getCategory(), 0L);
                if (aCnt != bCnt) return Long.compare(bCnt, aCnt);
                return Integer.compare(b.getViewCount(), a.getViewCount());
            });
            int n = Math.min(limit, candidates.size());
            for (int i = 0; i < n; i++) {
                Product p = candidates.get(i);
                personalized.add(buildItem(p, categoryCount, history.size(), true));
            }
        }

        // 4. 版块二：全站热门
        Set<Long> excludeIds = new HashSet<>(viewedIds);
        for (Map<String, Object> item : personalized) {
            excludeIds.add(((Number) item.get("productId")).longValue());
        }
        List<Product> hotProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, "ON_SALE")
                        .ne(Product::getUserId, userId)
                        .orderByDesc(Product::getViewCount)
                        .last("LIMIT 30"));
        List<Map<String, Object>> hot = new ArrayList<>();
        for (Product p : hotProducts) {
            if (excludeIds.add(p.getId())) {
                hot.add(buildItem(p, categoryCount, history.size(), false));
            }
            if (hot.size() >= limit) break;
        }

        // 5. 分析
        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("totalBrowses", history.size());
        analysis.put("uniqueProducts", viewedIds.size());
        List<Map<String, Object>> catList = new ArrayList<>();
        long maxCount = categoryCount.values().stream().max(Long::compareTo).orElse(1L);
        for (Map.Entry<String, Long> entry : categoryCount.entrySet()) {
            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("category", entry.getKey());
            cat.put("count", entry.getValue());
            cat.put("percentage", Math.round(entry.getValue() * 100.0 / maxCount));
            catList.add(cat);
        }
        analysis.put("topCategories", catList);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analysis", analysis);
        result.put("personalized", personalized);
        result.put("hot", hot);
        return Result.ok(result);
    }

    private void ensureTable() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `browse_history` (
                    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
                    `user_id`     BIGINT   NOT NULL,
                    `product_id`  BIGINT   NOT NULL,
                    `category`    VARCHAR(50) NOT NULL,
                    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (`id`),
                    KEY `idx_user_id` (`user_id`),
                    KEY `idx_category` (`category`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        } catch (Exception ignored) {}
    }

    private Map<String, Object> buildItem(Product p, Map<String, Long> categoryCount,
                                           int totalBrowses, boolean personalized) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("productId", p.getId());
        item.put("title", p.getTitle());
        item.put("price", p.getPrice());
        item.put("category", p.getCategory());
        item.put("viewCount", p.getViewCount());
        long catFreq = categoryCount.getOrDefault(p.getCategory(), 0L);
        if (personalized && catFreq > 0) {
            double score = 0.70 + Math.min(catFreq * 0.06, 0.25) + Math.random() * 0.04;
            item.put("score", Math.min(score, 0.98));
            item.put("reason", buildReason(p.getCategory(), catFreq, totalBrowses));
        } else {
            item.put("score", 0.60 + Math.random() * 0.25);
            item.put("reason", "全站热搜 · 高浏览量");
        }
        return item;
    }

    private String buildReason(String category, long catFreq, int totalBrowses) {
        String cn = catName(category);
        double ratio = (double) catFreq / totalBrowses;
        if (ratio >= 0.6) return "你主要在逛「" + cn + "」，猜你喜欢";
        if (ratio >= 0.3) return "你经常浏览「" + cn + "」，为你推荐";
        return "你看过「" + cn + "」，可能感兴趣";
    }

    private String catName(String category) {
        return switch (category) {
            case "BOOK" -> "书籍";
            case "ELECTRONICS" -> "电子产品";
            case "LIFESTYLE" -> "生活用品";
            case "SPORTS" -> "运动用品";
            default -> "其他";
        };
    }

    public Result<Map<String, Object>> suggestPrice(String category, String conditionLevel) {
        List<Product> similar = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getCategory, category)
                        .eq(Product::getConditionLevel, conditionLevel)
                        .eq(Product::getStatus, "ON_SALE"));

        double avgPrice = similar.stream()
                .map(Product::getPrice)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("category", category);
        result.put("conditionLevel", conditionLevel);
        result.put("marketAvgPrice", BigDecimal.valueOf(avgPrice).setScale(2, RoundingMode.HALF_UP));
        result.put("suggestedPrice", BigDecimal.valueOf(avgPrice * 0.95).setScale(2, RoundingMode.HALF_UP));
        result.put("sampleCount", similar.size());
        result.put("rangeLow", BigDecimal.valueOf(avgPrice * 0.7).setScale(2, RoundingMode.HALF_UP));
        result.put("rangeHigh", BigDecimal.valueOf(avgPrice * 1.3).setScale(2, RoundingMode.HALF_UP));
        return Result.ok(result);
    }
}
