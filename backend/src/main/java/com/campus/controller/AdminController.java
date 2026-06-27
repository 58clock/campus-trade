package com.campus.controller;

import com.campus.common.PageResult;
import com.campus.common.Result;
import com.campus.dto.StatisticsVO;
import com.campus.dto.UserVO;
import com.campus.dto.OrderVO;
import com.campus.dto.ProductVO;
import com.campus.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台管理", description = "管理员接口")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ================== 用户管理 ==================

    @Operation(summary = "用户列表")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public Result<PageResult<UserVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return adminService.listUsers(page, size, keyword, status);
    }

    @Operation(summary = "封禁用户")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}/ban")
    public Result<Void> banUser(@PathVariable Long id) {
        return adminService.banUser(id);
    }

    @Operation(summary = "解封用户")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}/unban")
    public Result<Void> unbanUser(@PathVariable Long id) {
        return adminService.unbanUser(id);
    }

    @Operation(summary = "重置用户密码")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        return adminService.resetPassword(id);
    }

    // ================== 商品审核 ==================

    @Operation(summary = "所有商品列表(审核)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/products")
    public Result<PageResult<ProductVO>> listProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return adminService.listAllProducts(page, size, keyword, status);
    }

    @Operation(summary = "强制下架商品")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/products/{id}/off-shelf")
    public Result<Void> forceOffShelf(@PathVariable Long id) {
        return adminService.forceOffShelf(id);
    }

    @Operation(summary = "删除商品")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        return adminService.deleteProduct(id);
    }

    // ================== 举报处理 ==================

    @Operation(summary = "举报列表")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reports")
    public Result<PageResult<com.campus.dto.ReportVO>> listReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String status) {
        return adminService.listReports(page, size, status);
    }

    @Operation(summary = "处理举报")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reports/{id}/handle")
    public Result<Void> handleReport(@PathVariable Long id,
                                      @RequestParam String action,
                                      @RequestParam(required = false) String note) {
        return adminService.handleReport(id, action, note);
    }

    // ================== 数据统计 ==================

    @Operation(summary = "数据统计面板")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/statistics")
    public Result<StatisticsVO> getStatistics() {
        return adminService.getStatistics();
    }

    // ================== 订单管理 ==================

    @Operation(summary = "所有订单列表")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/orders")
    public Result<PageResult<OrderVO>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return adminService.listAllOrders(page, size, keyword, status);
    }

    @Operation(summary = "管理员取消订单")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/orders/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        return adminService.cancelOrder(id);
    }
}
