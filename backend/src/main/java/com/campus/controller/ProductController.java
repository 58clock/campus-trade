package com.campus.controller;

import com.campus.common.PageResult;
import com.campus.common.Result;
import com.campus.dto.ProductVO;
import com.campus.dto.ProductListRequest;
import com.campus.dto.CreateProductRequest;
import com.campus.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "商品", description = "商品发布与管理接口")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "商品列表(公开)", description = "支持关键词搜索、分类筛选、价格区间、排序、分页")
    @GetMapping
    public Result<PageResult<ProductVO>> list(@Valid ProductListRequest query,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "12") int size) {
        return productService.listProducts(query, page, size);
    }

    @Operation(summary = "商品详情(公开)")
    @GetMapping("/{id}")
    public Result<ProductVO> getById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @Operation(summary = "发布商品")
    @PostMapping
    public Result<ProductVO> create(Authentication auth,
                                     @RequestParam("title") String title,
                                     @RequestParam(value = "description", required = false) String description,
                                     @RequestParam("category") String category,
                                     @RequestParam("price") String price,
                                     @RequestParam(value = "originalPrice", required = false) String originalPrice,
                                     @RequestParam("conditionLevel") String conditionLevel,
                                     @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        return productService.createProduct(getUserId(auth), title, description,
                category, price, originalPrice, conditionLevel, images);
    }

    @Operation(summary = "编辑商品")
    @PutMapping("/{id}")
    public Result<ProductVO> update(Authentication auth, @PathVariable Long id,
                                     @Valid @RequestBody CreateProductRequest request) {
        return productService.updateProduct(getUserId(auth), id, request);
    }

    @Operation(summary = "下架商品")
    @PutMapping("/{id}/off-shelf")
    public Result<Void> offShelf(Authentication auth, @PathVariable Long id) {
        return productService.offShelf(getUserId(auth), id);
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(Authentication auth, @PathVariable Long id) {
        return productService.deleteProduct(getUserId(auth), id);
    }

    @Operation(summary = "我发布的商品")
    @GetMapping("/my")
    public Result<PageResult<ProductVO>> myProducts(Authentication auth,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "12") int size) {
        return productService.getMyProducts(getUserId(auth), page, size);
    }

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
