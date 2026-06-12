package com.campus.service;

import com.campus.common.PageResult;
import com.campus.common.Result;
import com.campus.dto.CreateProductRequest;
import com.campus.dto.ProductListRequest;
import com.campus.dto.ProductVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ProductService {

    // ==================== [桩] B 实现的真实接口 ====================

    public Result<PageResult<ProductVO>> listProducts(ProductListRequest query, int page, int size) {
        // TODO: B - 实现搜索、筛选、排序、分页
        // MyBatis-Plus: LambdaQueryWrapper + Page<Product>
        // 关键词匹配 title/description
        // 分类筛选、价格区间、成色过滤
        // 排序: price_asc / price_desc / newest
        return Result.ok(PageResult.empty());
    }

    public Result<ProductVO> getProductById(Long id) {
        // TODO: B - 查询商品详情，同时 view_count +1
        // 关联查询卖家信息 (Join user 表)
        // 解析 images JSON 字符串为 List<String>
        return Result.ok(new ProductVO());
    }

    public Result<ProductVO> createProduct(Long userId, String title, String description,
                                            String category, String price, String originalPrice,
                                            String conditionLevel, List<MultipartFile> images) {
        // TODO: B - 保存商品，多图上传至 /uploads/products/，存 JSON 数组到 images 字段
        return Result.ok(new ProductVO());
    }

    public Result<ProductVO> updateProduct(Long userId, Long id, CreateProductRequest request) {
        // TODO: B - 校验本人操作，更新商品信息
        return Result.ok(new ProductVO());
    }

    public Result<Void> offShelf(Long userId, Long id) {
        // TODO: B - 校验本人操作，改状态为 OFF_SHELF
        return Result.ok();
    }

    public Result<Void> deleteProduct(Long userId, Long id) {
        // TODO: B - 校验本人操作，软删除（改状态为 DELETED）
        return Result.ok();
    }

    public Result<PageResult<ProductVO>> getMyProducts(Long userId, int page, int size) {
        // TODO: B - 查当前用户的所有商品（排除 DELETED），分页返回
        return Result.ok(PageResult.empty());
    }
}
