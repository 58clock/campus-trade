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
        // 1. 查询浏览历史
        List<BrowseHistory> history = queryHistory(userId);
        log.info("Browse history for userId={}: {} records", userId, history.size());

        // 2. 获取用户浏览过的商品标题
        Set<Long> viewedProductIds = history.stream()
                .map(BrowseHistory::getProductId).collect(Collectors.toSet());
        List<Product> viewedProducts = viewedProductIds.isEmpty() ? List.of()
                : productMapper.selectBatchIds(viewedProductIds);

        // 3. 统计分类
        Map<String, Long> categoryCount = history.stream()
                .collect(Collectors.groupingBy(BrowseHistory::getCategory, LinkedHashMap::new, Collectors.counting()));

        // 4. 版块一：猜你喜欢 — 用浏览商品的标题做模糊搜索
        List<Map<String, Object>> personalized = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        if (!viewedProducts.isEmpty()) {
            for (Product viewed : viewedProducts) {
                // 从标题提取关键词（取前几个字做模糊匹配）
                String kw = extractKeyword(viewed.getTitle());
                if (kw.isEmpty()) continue;

                List<Product> matches = productMapper.selectList(
                        new LambdaQueryWrapper<Product>()
                                .eq(Product::getStatus, "ON_SALE")
                                .ne(Product::getUserId, userId)
                                .like(Product::getTitle, kw)
                                .orderByDesc(Product::getViewCount));
                for (Product p : matches) {
                    if (!viewedProductIds.contains(p.getId()) && seen.add(p.getId())) {
                        long freq = categoryCount.getOrDefault(p.getCategory(), 0L);
                        personalized.add(buildItem(p, freq, history.size(), true));
                    }
                }
            }
        }

        // 5. 版块二：全站热门 — 简单取浏览量最高的 N 个
        List<Product> hotProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, "ON_SALE")
                        .ne(Product::getUserId, userId)
                        .orderByDesc(Product::getViewCount));
        List<Map<String, Object>> hot = new ArrayList<>();
        for (Product p : hotProducts) {
            if (hot.size() >= limit) break;
            hot.add(buildItem(p, 0, history.size(), false));
        }

        // 6. 分析
        Map<String, Object> analysis = buildAnalysis(history, categoryCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analysis", analysis);
        result.put("personalized", personalized);
        result.put("hot", hot);
        log.info("Recommend: personalized={}, hot={}", personalized.size(), hot.size());
        return Result.ok(result);
    }

    // === helper methods ===

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

    /** 从标题中提取关键词做模糊搜索 */
    private String extractKeyword(String title) {
        if (title == null || title.isBlank()) return "";
        // 去掉括号、特殊符号，取前 2-6 个字作为关键词
        String cleaned = title.replaceAll("[（(][^)）]*[)）]|[\\[\\]【】\\-—·◆★]", "");
        // 尝试取有意义的片段：2-4 个汉字
        if (cleaned.length() >= 2) return cleaned.substring(0, Math.min(6, cleaned.length()));
        return cleaned;
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
            item.put("reason", "与你浏览过的「" + p.getTitle().substring(0, Math.min(6, p.getTitle().length())) + "…」相似");
        } else {
            item.put("score", 0.60 + Math.random() * 0.25);
            item.put("reason", "全站热搜 · 高浏览量");
        }
        return item;
    }

    // === pricing ===

    public Result<Map<String, Object>> suggestPrice(String category, String conditionLevel) {
        List<Product> similar = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getCategory, category)
                        .eq(Product::getConditionLevel, conditionLevel)
                        .eq(Product::getStatus, "ON_SALE"));
        double avg = similar.stream()
                .map(Product::getPrice).filter(Objects::nonNull)
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
