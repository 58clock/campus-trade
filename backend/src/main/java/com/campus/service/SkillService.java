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

    public Result<List<Map<String, Object>>> recommend(Long userId, int limit) {
        // 1. 查询用户浏览历史，按分类统计浏览次数
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
            // 无浏览历史：推荐全站热门商品
            return recommendHot(userId, limit);
        }

        // 2. 统计每个分类的浏览次数
        Map<String, Long> categoryCount = history.stream()
                .collect(Collectors.groupingBy(BrowseHistory::getCategory, LinkedHashMap::new, Collectors.counting()));

        // 3. 获取用户浏览过的商品ID，避免重复推荐
        Set<Long> viewedIds = history.stream()
                .map(BrowseHistory::getProductId)
                .collect(Collectors.toSet());

        // 4. 在每个分类下找热门商品
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

        // 5. 去重 + 按分类频次和浏览量综合排序
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
            // 不足时用热门商品补齐
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

        List<Map<String, Object>> result = new ArrayList<>();
        int count = Math.min(limit, ranked.size());
        for (int i = 0; i < count; i++) {
            Product p = ranked.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", p.getId());
            item.put("title", p.getTitle());
            item.put("price", p.getPrice());
            item.put("category", p.getCategory());
            item.put("viewCount", p.getViewCount());
            // 推荐指数：浏览分类频次越高分数越高
            long catFreq = categoryCount.getOrDefault(p.getCategory(), 0L);
            double score = 0.6 + Math.min(catFreq * 0.08, 0.35) + Math.random() * 0.05;
            item.put("score", Math.min(score, 0.99));
            result.add(item);
        }
        return Result.ok(result);
    }

    private Result<List<Map<String, Object>>> recommendHot(Long userId, int limit) {
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, "ON_SALE")
                        .ne(Product::getUserId, userId)
                        .orderByDesc(Product::getViewCount)
                        .last("LIMIT " + limit));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", p.getId());
            item.put("title", p.getTitle());
            item.put("price", p.getPrice());
            item.put("category", p.getCategory());
            item.put("viewCount", p.getViewCount());
            item.put("score", 0.7 + Math.random() * 0.2);
            result.add(item);
        }
        return Result.ok(result);
    }

    public Result<Map<String, Object>> suggestPrice(String category, String conditionLevel) {
        // TODO: D - 基于同类同成色商品均价给出建议
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
