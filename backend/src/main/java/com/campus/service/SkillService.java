package com.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.Result;
import com.campus.entity.BrowseHistory;
import com.campus.entity.Product;
import com.campus.mapper.BrowseHistoryMapper;
import com.campus.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public Result<Map<String, Object>> recommend(Long userId, int limit) {
        // 1. 查浏览历史
        List<BrowseHistory> history = queryHistory(userId);

        // 2. 统计分类 + 已浏览ID
        Map<String, Long> categoryCount = history.stream()
                .collect(Collectors.groupingBy(BrowseHistory::getCategory, LinkedHashMap::new, Collectors.counting()));
        Set<Long> viewedIds = history.stream().map(BrowseHistory::getProductId).collect(Collectors.toSet());

        // 3. 全站 ON_SALE 且非本人的商品（公用的数据源）
        List<Product> allProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, "ON_SALE")
                        .ne(Product::getUserId, userId)
                        .orderByDesc(Product::getViewCount));

        // 4. 版块一：猜你喜欢
        List<Map<String, Object>> personalized = new ArrayList<>();
        Set<Long> used = new HashSet<>();

        if (!categoryCount.isEmpty()) {
            // 有浏览历史 → 从浏览过的分类中推荐
            String topCat = categoryCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).get().getKey();
            log.info("Top category for userId={}: {}", userId, topCat);

            for (Product p : allProducts) {
                if (p.getCategory().equals(topCat) && !viewedIds.contains(p.getId()) && used.size() < 3) {
                    used.add(p.getId());
                    personalized.add(buildItem(p, categoryCount.getOrDefault(p.getCategory(), 0L), history.size(), true));
                }
            }
            // 同分类不够3个，用其他分类补
            for (Product p : allProducts) {
                if (used.size() >= 3) break;
                if (!viewedIds.contains(p.getId()) && used.add(p.getId())) {
                    personalized.add(buildItem(p, categoryCount.getOrDefault(p.getCategory(), 0L), history.size(), true));
                }
            }
        }

        // 无论有没有浏览历史，至少塞3个
        if (personalized.isEmpty()) {
            for (Product p : allProducts) {
                if (used.size() >= 3) break;
                if (used.add(p.getId())) {
                    personalized.add(buildItem(p, 0, 0, true));
                }
            }
        }

        // 5. 版块二：全站热门（排除猜你喜欢已用的）
        List<Map<String, Object>> hot = new ArrayList<>();
        for (Product p : allProducts) {
            if (!used.contains(p.getId()) && hot.size() < limit) {
                hot.add(buildItem(p, 0, 0, false));
                used.add(p.getId());
            }
        }

        // 6. 分析 + 调试
        Map<String, Object> analysis = buildAnalysis(history, categoryCount);
        Map<String, Object> debug = buildDebug(history, allProducts, personalized, hot);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analysis", analysis);
        result.put("personalized", personalized);
        result.put("hot", hot);
        result.put("debug", debug);
        return Result.ok(result);
    }

    private List<BrowseHistory> queryHistory(Long userId) {
        try {
            List<BrowseHistory> h = browseHistoryMapper.selectList(
                    new LambdaQueryWrapper<BrowseHistory>()
                            .eq(BrowseHistory::getUserId, userId)
                            .orderByDesc(BrowseHistory::getCreatedAt));
            return h.size() > 100 ? h.subList(0, 100) : h;
        } catch (Exception e) {
            log.warn("Query browse_history failed: {}", e.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> buildItem(Product p, long catFreq, int total, boolean personalized) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("productId", p.getId());
        item.put("title", p.getTitle());
        item.put("price", p.getPrice());
        item.put("category", p.getCategory());
        item.put("viewCount", p.getViewCount());
        if (personalized && catFreq > 0) {
            double s = 0.70 + Math.min(catFreq * 0.06, 0.25) + Math.random() * 0.04;
            item.put("score", Math.min(s, 0.98));
            item.put("reason", "你浏览过「" + catName(p.getCategory()) + "」，为你推荐同类热门");
        } else if (personalized) {
            item.put("score", 0.55 + Math.random() * 0.15);
            item.put("reason", "精选" + catName(p.getCategory()) + "商品");
        } else {
            item.put("score", 0.60 + Math.random() * 0.25);
            item.put("reason", "全站热搜 · 高浏览量");
        }
        return item;
    }

    private Map<String, Object> buildAnalysis(List<BrowseHistory> history, Map<String, Long> cc) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("totalBrowses", history.size());
        a.put("uniqueProducts", history.stream().map(BrowseHistory::getProductId).distinct().count());
        List<Map<String, Object>> cats = new ArrayList<>();
        long max = cc.values().stream().max(Long::compareTo).orElse(1L);
        for (Map.Entry<String, Long> e : cc.entrySet()) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("category", e.getKey());
            c.put("count", e.getValue());
            c.put("percentage", Math.round(e.getValue() * 100.0 / max));
            cats.add(c);
        }
        a.put("topCategories", cats);
        return a;
    }

    private Map<String, Object> buildDebug(List<BrowseHistory> history, List<Product> all,
                                            List<Map<String, Object>> per, List<Map<String, Object>> hot) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("source_table", "browse_history");
        d.put("total_rows", history.size());
        d.put("all_products_count", all.size());
        d.put("personalized_count", per.size());
        d.put("hot_count", hot.size());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (BrowseHistory bh : history) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("userId", bh.getUserId());
            r.put("productId", bh.getProductId());
            r.put("category", bh.getCategory());
            r.put("createdAt", bh.getCreatedAt() != null ? bh.getCreatedAt().toString() : "null");
            rows.add(r);
        }
        d.put("rows", rows);
        return d;
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

    // === pricing ===

    public Result<Map<String, Object>> suggestPrice(String category, String conditionLevel) {
        List<Product> similar = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getCategory, category)
                        .eq(Product::getConditionLevel, conditionLevel)
                        .eq(Product::getStatus, "ON_SALE"));
        double avg = similar.stream().map(Product::getPrice).filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).average().orElse(0.0);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("category", category);
        r.put("conditionLevel", conditionLevel);
        r.put("marketAvgPrice", BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        r.put("suggestedPrice", BigDecimal.valueOf(avg * 0.95).setScale(2, RoundingMode.HALF_UP));
        r.put("sampleCount", similar.size());
        r.put("rangeLow", BigDecimal.valueOf(avg * 0.7).setScale(2, RoundingMode.HALF_UP));
        r.put("rangeHigh", BigDecimal.valueOf(avg * 1.3).setScale(2, RoundingMode.HALF_UP));
        return Result.ok(r);
    }
}
