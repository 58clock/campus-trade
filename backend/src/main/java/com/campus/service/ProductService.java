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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    // ==================== [任务 B 实现的真实接口] ====================

    public Result<PageResult<ProductVO>> listProducts(ProductListRequest query, int page, int size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, "ON_SALE");

        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.and(w -> w.like(Product::getTitle, query.getKeyword())
                    .or()
                    .like(Product::getDescription, query.getKeyword()));
        }
        if (query.getCategory() != null && !query.getCategory().isBlank()) {
            wrapper.eq(Product::getCategory, query.getCategory());
        }
        if (query.getConditionLevel() != null && !query.getConditionLevel().isBlank()) {
            wrapper.eq(Product::getConditionLevel, query.getConditionLevel());
        }
        if (query.getMinPrice() != null && query.getMinPrice().compareTo(BigDecimal.ZERO) > 0) {
            wrapper.ge(Product::getPrice, query.getMinPrice());
        }
        if (query.getMaxPrice() != null && query.getMaxPrice().compareTo(BigDecimal.ZERO) > 0) {
            wrapper.le(Product::getPrice, query.getMaxPrice());
        }

        if ("price_asc".equals(query.getSort())) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(query.getSort())) {
            wrapper.orderByDesc(Product::getPrice);
        } else {
            wrapper.orderByDesc(Product::getCreatedAt);
        }

        Page<Product> p = new Page<>(page, size);
        Page<Product> result = productMapper.selectPage(p, wrapper);

        PageResult<ProductVO> pageResult = new PageResult<>(
                result.getRecords().stream().map(this::toVO).collect(Collectors.toList()),
                result.getTotal(), page, size);
        return Result.ok(pageResult);
    }

    public Result<ProductVO> getProductById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        if (!"ON_SALE".equals(product.getStatus())) {
            return Result.fail("商品已下架或删除");
        }
        product.setViewCount(product.getViewCount() == null ? 1 : product.getViewCount() + 1);
        productMapper.updateById(product);
        return Result.ok(toVO(product));
    }

    public Result<ProductVO> createProduct(Long userId, String title, String description,
                                           String category, String price, String originalPrice,
                                           String conditionLevel, List<MultipartFile> images) {
        Product product = new Product();
        product.setUserId(userId);
        product.setTitle(title);
        product.setDescription(description);
        product.setCategory(category);
        product.setPrice(new BigDecimal(price));
        if (originalPrice != null && !originalPrice.isBlank()) {
            product.setOriginalPrice(new BigDecimal(originalPrice));
        }
        product.setConditionLevel(conditionLevel);
        product.setStatus("ON_SALE");
        product.setViewCount(0);
        product.setCreatedAt(java.time.LocalDateTime.now());
        product.setUpdatedAt(java.time.LocalDateTime.now());

        if (images != null && !images.isEmpty()) {
            List<String> urls = new ArrayList<>();
            for (MultipartFile file : images) {
                if (file.isEmpty()) continue;
                try {
                    String originalFilename = file.getOriginalFilename();
                    String ext = originalFilename != null && originalFilename.contains(".")
                            ? originalFilename.substring(originalFilename.lastIndexOf("."))
                            : ".jpg";
                    String filename = "product_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

                    //Path productDir = Paths.get(uploadPath, "products");
                    Path productDir = Paths.get(System.getProperty("user.dir"), uploadPath, "products");
                    Files.createDirectories(productDir);
                    Path filepath = productDir.resolve(filename);
                    file.transferTo(filepath.toFile());

                    urls.add("/uploads/products/" + filename);
                } catch (IOException e) {
                    return Result.fail("图片上传失败: " + e.getMessage());
                }
            }
            try {
                product.setImages(objectMapper.writeValueAsString(urls));
            } catch (JsonProcessingException e) {
                return Result.fail("图片数据序列化失败");
            }
        }

        productMapper.insert(product);
        return Result.ok(toVO(product));
    }

    public Result<ProductVO> updateProduct(Long userId, Long id, CreateProductRequest request) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        if (!product.getUserId().equals(userId)) {
            return Result.fail("无权操作该商品");
        }
        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        if (request.getOriginalPrice() != null) {
            product.setOriginalPrice(request.getOriginalPrice());
        }
        product.setConditionLevel(request.getConditionLevel());
        productMapper.updateById(product);
        return Result.ok(toVO(product));
    }

    public Result<Void> offShelf(Long userId, Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        if (!product.getUserId().equals(userId)) {
            return Result.fail("无权操作该商品");
        }
        product.setStatus("OFF_SHELF");
        productMapper.updateById(product);
        return Result.ok();
    }

    public Result<Void> deleteProduct(Long userId, Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        if (!product.getUserId().equals(userId)) {
            return Result.fail("无权操作该商品");
        }
        product.setStatus("DELETED");
        productMapper.updateById(product);
        return Result.ok();
    }

    public Result<PageResult<ProductVO>> getMyProducts(Long userId, int page, int size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getUserId, userId)
                .ne(Product::getStatus, "DELETED")
                .orderByDesc(Product::getCreatedAt);

        Page<Product> p = new Page<>(page, size);
        Page<Product> result = productMapper.selectPage(p, wrapper);

        PageResult<ProductVO> pageResult = new PageResult<>(
                result.getRecords().stream().map(this::toVO).collect(Collectors.toList()),
                result.getTotal(), page, size);
        return Result.ok(pageResult);
    }

    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setUserId(product.getUserId());
        vo.setTitle(product.getTitle());
        vo.setDescription(product.getDescription());
        vo.setCategory(product.getCategory());
        vo.setPrice(product.getPrice());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setConditionLevel(product.getConditionLevel());
        vo.setStatus(product.getStatus());
        vo.setViewCount(product.getViewCount());
        vo.setCreatedAt(product.getCreatedAt());

        if (product.getImages() != null && !product.getImages().isBlank()) {
            try {
                List<String> imageList = objectMapper.readValue(product.getImages(), new TypeReference<List<String>>() {});
                vo.setImages(imageList);
            } catch (JsonProcessingException e) {
                vo.setImages(List.of());
            }
        } else {
            vo.setImages(List.of());
        }

        User seller = userMapper.selectById(product.getUserId());
        if (seller != null) {
            vo.setSellerName(seller.getNickname());
            vo.setSellerAvatar(seller.getAvatar());
        }

        return vo;
    }
}