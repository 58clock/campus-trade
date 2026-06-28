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
        ensureTable();

        // 1. 查询浏览历史
        List<BrowseHistory> history;
        try {
            history = browseHistoryMapper.selectList(
                    new LambdaQueryWrapper<BrowseHistory>()
                            .eq(BrowseHistory::getUserId, userId)
                            .orderByDesc(BrowseHistory::getCreatedAt));
            // Java 端截断，不用 SQL LIMIT（避免 PaginationInterceptor 冲突）
            if (history.size() > 100) history = history.subList(0, 100);
        } catch (Exception e) {
            log.warn("Query browse_history failed: {}", e.getMessage());
            history = List.of();
        }
        log.info("Browse history for userId={}: {} records", userId, history.size());

        // 2. 统计
        Map<String, Long> categoryCount = history.stream()
                .collect(Collectors.groupingBy(BrowseHistory::getCategory, LinkedHashMap::new, Collectors.counting()));
        Set<Long> viewedIds = history.stream()
                .map(BrowseHistory::getProductId).collect(Collectors.toSet());

        // 3. 版块一：猜你喜欢
        List<Map<String, Object>> personalized = new ArrayList<>();
        Set<Long> excludeIds = new HashSet<>(viewedIds);
        if (!history.isEmpty()) {
            // 取前几个最频繁的分类
            List<String> topCats = categoryCount.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .toList();

            List<Product> candidates = new ArrayList<>();
            for (String cat : topCats) {
                List<Product> catProducts = productMapper.selectList(
                        new LambdaQueryWrapper<Product>()
                                .eq(Product::getCategory, cat)
                                .eq(Product::getStatus, "ON_SALE")
                                .ne(Product::getUserId, userId)
                                .orderByDesc(Product::getViewCount));
                for (Product p : catProducts) {
                    if (!viewedIds.contains(p.getId())) {
                        candidates.add(p);
                    }
                }
            }

            // 按分类频次排序
            candidates.sort((a, b) -> {
                long aCnt = categoryCount.getOrDefault(a.getCategory(), 0L);
                long bCnt = categoryCount.getOrDefault(b.getCategory(), 0L);
                if (aCnt != bCnt) return Long.compare(bCnt, aCnt);
                return Integer.compare(b.getViewCount(), a.getViewCount());
            });

            for (int i = 0; i < Math.min(limit, candidates.size()); i++) {
                Product p = candidates.get(i);
                excludeIds.add(p.getId());
                personalized.add(buildItem(p, categoryCount, history.size(), true));
            }
        }

        // 4. 版块二：全站热门 — 不用 .last()，Java 端截断
        List<Product> allOnSale = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, "ON_SALE")
                        .ne(Product::getUserId, userId)
                        .orderByDesc(Product::getViewCount));

        List<Map<String, Object>> hot = new ArrayList<>();
        for (Product p : allOnSale) {
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
        for (Map.Entry<String, Long> e : categoryCount.entrySet()) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("category", e.getKey());
            c.put("count", e.getValue());
            c.put("percentage", Math.round(e.getValue() * 100.0 / maxCount));
            catList.add(c);
        }
        analysis.put("topCategories", catList);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analysis", analysis);
        result.put("personalized", personalized);
        result.put("hot", hot);
        log.info("Recommend result: personalized={}, hot={}", personalized.size(), hot.size());
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

    private Map<String, Object> buildItem(Product p, Map<String, Long> cc,
                                           int total, boolean personalized) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("productId", p.getId());
        item.put("title", p.getTitle());
        item.put("price", p.getPrice());
        item.put("category", p.getCategory());
        item.put("viewCount", p.getViewCount());

        long freq = cc.getOrDefault(p.getCategory(), 0L);
        if (personalized && freq > 0) {
            double score = 0.70 + Math.min(freq * 0.06, 0.25) + Math.random() * 0.04;
            item.put("score", Math.min(score, 0.98));
            item.put("reason", reason(p.getCategory(), freq, total));
        } else {
            item.put("score", 0.60 + Math.random() * 0.25);
            item.put("reason", "全站热搜 · 高浏览量");
        }
        return item;
    }

    private String reason(String cat, long freq, int total) {
        String cn = catName(cat);
        double r = (double) freq / total;
        if (r >= 0.6) return "你主要在逛「" + cn + "」，猜你喜欢";
        if (r >= 0.3) return "你经常浏览「" + cn + "」，为你推荐";
        return "你看过「" + cn + "」，可能感兴趣";
    }

    private String catName(String c) {
        return switch (c) {
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
                .map(Product::getPrice).filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).average().orElse(0.0);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("category", category);
        r.put("conditionLevel", conditionLevel);
        r.put("marketAvgPrice", BigDecimal.valueOf(avgPrice).setScale(2, RoundingMode.HALF_UP));
        r.put("suggestedPrice", BigDecimal.valueOf(avgPrice * 0.95).setScale(2, RoundingMode.HALF_UP));
        r.put("sampleCount", similar.size());
        r.put("rangeLow", BigDecimal.valueOf(avgPrice * 0.7).setScale(2, RoundingMode.HALF_UP));
        r.put("rangeHigh", BigDecimal.valueOf(avgPrice * 1.3).setScale(2, RoundingMode.HALF_UP));
        return Result.ok(r);
    }
}
