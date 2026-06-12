package com.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.Result;
import com.campus.entity.Product;
import com.campus.entity.Review;
import com.campus.mapper.ProductMapper;
import com.campus.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final ProductMapper productMapper;
    private final ReviewMapper reviewMapper;

    public Result<List<Map<String, Object>>> recommend(Long userId, int limit) {
        // TODO: D - 简单推荐逻辑：同分类热门商品（按浏览量倒序）
        // 1. 查用户发布/购买过的商品分类
        // 2. 在该分类下推荐高浏览量商品
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
            item.put("score", 0.8 + Math.random() * 0.2);
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
