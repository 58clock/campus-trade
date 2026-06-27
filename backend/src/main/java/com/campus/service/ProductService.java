package com.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.PageResult;
import com.campus.common.Result;
import com.campus.dto.CreateProductRequest;
import com.campus.dto.ProductListRequest;
import com.campus.dto.ProductVO;
import com.campus.entity.Product;
import com.campus.entity.User;
import com.campus.mapper.ProductMapper;
import com.campus.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    // ==================== [桩] B 实现的真实接口 ====================

    public Result<PageResult<ProductVO>> listProducts(ProductListRequest query, int page, int size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, "ON_SALE");

        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.and(w -> w.like(Product::getTitle, query.getKeyword())
                    .or().like(Product::getDescription, query.getKeyword()));
        }
        if (query.getCategory() != null && !query.getCategory().isBlank()) {
            wrapper.eq(Product::getCategory, query.getCategory());
        }
        if (query.getMinPrice() != null) {
            wrapper.ge(Product::getPrice, query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            wrapper.le(Product::getPrice, query.getMaxPrice());
        }

        String sort = query.getSort();
        if ("price_asc".equals(sort)) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(sort)) {
            wrapper.orderByDesc(Product::getPrice);
        } else {
            wrapper.orderByDesc(Product::getCreatedAt);
        }

        Page<Product> p = new Page<>(page, size);
        Page<Product> result = productMapper.selectPage(p, wrapper);

        List<ProductVO> records = result.getRecords().stream().map(prod -> {
            ProductVO vo = new ProductVO();
            vo.setId(prod.getId());
            vo.setUserId(prod.getUserId());
            vo.setTitle(prod.getTitle());
            vo.setDescription(prod.getDescription());
            vo.setCategory(prod.getCategory());
            vo.setPrice(prod.getPrice());
            vo.setOriginalPrice(prod.getOriginalPrice());
            vo.setConditionLevel(prod.getConditionLevel());
            vo.setViewCount(prod.getViewCount());
            vo.setStatus(prod.getStatus());
            vo.setCreatedAt(prod.getCreatedAt());
            User seller = userMapper.selectById(prod.getUserId());
            if (seller != null) {
                vo.setSellerName(seller.getNickname());
                vo.setSellerAvatar(seller.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());

        return Result.ok(new PageResult<>(records, result.getTotal(), page, size));
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
