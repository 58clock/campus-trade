package com.campus.controller;

import com.campus.common.PageResult;
import com.campus.common.Result;
import com.campus.dto.OrderVO;
import com.campus.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单", description = "交易与订单接口")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单(买家下单)")
    @PostMapping
    public Result<OrderVO> create(Authentication auth, @RequestParam Long productId) {
        return orderService.createOrder(getUserId(auth), productId);
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> getById(Authentication auth, @PathVariable Long id) {
        return orderService.getOrderById(getUserId(auth), id);
    }

    @Operation(summary = "模拟付款(买家操作)")
    @PutMapping("/{id}/pay")
    public Result<Void> pay(Authentication auth, @PathVariable Long id) {
        return orderService.pay(getUserId(auth), id);
    }

    @Operation(summary = "确认发货(卖家操作)")
    @PutMapping("/{id}/ship")
    public Result<Void> ship(Authentication auth, @PathVariable Long id) {
        return orderService.ship(getUserId(auth), id);
    }

    @Operation(summary = "确认收货(买家操作)")
    @PutMapping("/{id}/receive")
    public Result<Void> receive(Authentication auth, @PathVariable Long id) {
        return orderService.receive(getUserId(auth), id);
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(Authentication auth, @PathVariable Long id,
                                @RequestParam(required = false) String reason) {
        return orderService.cancel(getUserId(auth), id, reason);
    }

    @Operation(summary = "我的订单(作为买家)")
    @GetMapping("/bought")
    public Result<PageResult<OrderVO>> bought(Authentication auth,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "12") int size,
                                               @RequestParam(required = false) String status) {
        return orderService.getBoughtOrders(getUserId(auth), page, size, status);
    }

    @Operation(summary = "我的订单(作为卖家)")
    @GetMapping("/sold")
    public Result<PageResult<OrderVO>> sold(Authentication auth,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "12") int size,
                                             @RequestParam(required = false) String status) {
        return orderService.getSoldOrders(getUserId(auth), page, size, status);
    }

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
