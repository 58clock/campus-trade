package com.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.Result;
import com.campus.entity.BrowseHistory;
import com.campus.entity.Product;
import com.campus.mapper.BrowseHistoryMapper;
import com.campus.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final ProductMapper productMapper;
    private final BrowseHistoryMapper browseHistoryMapper;

    public Result<Map<String, Object>> recommend(Long userId, int limit) {
        List<BrowseHistory> history;
        try {
            history = browseHistoryMapper.selectList(
                    new LambdaQueryWrapper<BrowseHistory>()
                            .eq(BrowseHistory::getUserId, userId)
                            .orderByDesc(BrowseHistory::getCreatedAt)
                            .last("LIMIT 100"));
        } catch (Exception e) {
            return recommendHot(userId, limit);
        }

        if (history.isEmpty()) {
            return recommendHot(userId, limit);
        }

        // 统计每个分类的浏览次数
        Map<String, Long> categoryCount = history.stream()
                .collect(Collectors.groupingBy(BrowseHistory::getCategory, LinkedHashMap::new, Collectors.counting()));

        // 用户浏览过的商品ID
        Set<Long> viewedIds = history.stream()
                .map(BrowseHistory::getProductId)
                .collect(Collectors.toSet());

        // 用户浏览的价格范围
        Set<Long> browsedProductIds = new HashSet<>(viewedIds);
        List<Product> browsedProducts = productMapper.selectBatchIds(browsedProductIds);
        BigDecimal browsePriceMin = null, browsePriceMax = null;
        if (!browsedProducts.isEmpty()) {
            browsePriceMin = browsedProducts.stream()
                    .map(Product::getPrice).filter(Objects::nonNull)
                    .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            browsePriceMax = browsedProducts.stream()
                    .map(Product::getPrice).filter(Objects::nonNull)
                    .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        }

        // 在每个分类下找热门商品
        List<Product> candidates = new ArrayList<>();
        for (String category : categoryCount.keySet()) {
            List<Product> categoryProducts = productMapper.selectList(
                    new LambdaQueryWrapper<Product>()
                            .eq(Product::getCategory, category)
                            .eq(Product::getStatus, "ON_SALE")
                            .ne(Product::getUserId, userId)
                            .orderByDesc(Product::getViewCount)
                            .last("LIMIT 10"));
            candidates.addAll(categoryProducts);
        }

        // 去重 + 按分类频次和浏览量综合排序
        Set<Long> seen = new HashSet<>();
        List<Product> ranked = new ArrayList<>();
        for (Product p : candidates) {
            if (seen.add(p.getId()) && !viewedIds.contains(p.getId())) {
                ranked.add(p);
            }
        }

        ranked.sort((a, b) -> {
            long aCnt = categoryCount.getOrDefault(a.getCategory(), 0L);
            long bCnt = categoryCount.getOrDefault(b.getCategory(), 0L);
            if (aCnt != bCnt) return Long.compare(bCnt, aCnt);
            return Integer.compare(b.getViewCount(), a.getViewCount());
        });

        if (ranked.size() < limit) {
            List<Product> hot = productMapper.selectList(
                    new LambdaQueryWrapper<Product>()
                            .eq(Product::getStatus, "ON_SALE")
                            .ne(Product::getUserId, userId)
                            .orderByDesc(Product::getViewCount)
                            .last("LIMIT 30"));
            for (Product p : hot) {
                if (seen.add(p.getId())) {
                    ranked.add(p);
                }
                if (ranked.size() >= limit) break;
            }
        }

        // 构建分析数据
        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("totalBrowses", history.size());
        analysis.put("browseCount", history.size());
        analysis.put("uniqueProducts", browsedProductIds.size());

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

        if (browsePriceMin != null) {
            analysis.put("priceRange", browsePriceMin + " ~ " + browsePriceMax);
        }

        // 构建商品列表，每个商品带推荐理由
        List<Map<String, Object>> items = new ArrayList<>();
        int count = Math.min(limit, ranked.size());
        for (int i = 0; i < count; i++) {
            Product p = ranked.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", p.getId());
            item.put("title", p.getTitle());
            item.put("price", p.getPrice());
            item.put("category", p.getCategory());
            item.put("viewCount", p.getViewCount());

            long catFreq = categoryCount.getOrDefault(p.getCategory(), 0L);
            double score = 0.6 + Math.min(catFreq * 0.08, 0.35) + Math.random() * 0.05;
            item.put("score", Math.min(score, 0.99));

            // 推荐理由
            item.put("reason", buildReason(p.getCategory(), catFreq, history.size()));

            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analysis", analysis);
        result.put("items", items);
        return Result.ok(result);
    }

    private String buildReason(String category, long catFreq, int totalBrowses) {
        String catName = switch (category) {
            case "BOOK" -> "书籍";
            case "ELECTRONICS" -> "电子产品";
            case "LIFESTYLE" -> "生活用品";
            case "SPORTS" -> "运动用品";
            default -> "其他";
        };
        if (catFreq >= totalBrowses * 0.6) {
            return "你最近主要浏览" + catName + "，这是该分类的热门商品";
        } else if (catFreq >= totalBrowses * 0.3) {
            return "你经常浏览" + catName + "，为你推荐同类商品";
        } else {
            return "你浏览过" + catName + "商品，猜你喜欢";
        }
    }

    private Result<Map<String, Object>> recommendHot(Long userId, int limit) {
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, "ON_SALE")
                        .ne(Product::getUserId, userId)
                        .orderByDesc(Product::getViewCount)
                        .last("LIMIT " + limit));

        List<Map<String, Object>> items = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", p.getId());
            item.put("title", p.getTitle());
            item.put("price", p.getPrice());
            item.put("category", p.getCategory());
            item.put("viewCount", p.getViewCount());
            item.put("score", 0.7 + Math.random() * 0.2);
            item.put("reason", "全站热门商品，暂无你的浏览记录");
            items.add(item);
        }

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("mode", "hot");
        analysis.put("totalBrowses", 0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analysis", analysis);
        result.put("items", items);
        return Result.ok(result);
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
