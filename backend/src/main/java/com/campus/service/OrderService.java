package com.campus.service;

import com.campus.common.PageResult;
import com.campus.common.Result;
import com.campus.dto.OrderVO;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    // ==================== [桩] C 实现的真实接口 ====================

    public Result<OrderVO> createOrder(Long buyerId, Long productId) {
        // TODO: C - 创建订单，校验商品状态，不能自己买自己的，商品状态改为 LOCKED
        // 初始状态 PENDING_PAYMENT
        return Result.ok(new OrderVO());
    }

    public Result<OrderVO> getOrderById(Long userId, Long id) {
        // TODO: C - 查询订单详情，校验是买家或卖家
        return Result.ok(new OrderVO());
    }

    public Result<Void> pay(Long userId, Long id) {
        // TODO: C - 模拟付款，校验是买家 + 状态是 PENDING_PAYMENT → PENDING_SHIPMENT
        return Result.ok();
    }

    public Result<Void> ship(Long userId, Long id) {
        // TODO: C - 卖家确认发货，PENDING_SHIPMENT → PENDING_RECEIPT
        return Result.ok();
    }

    public Result<Void> receive(Long userId, Long id) {
        // TODO: C - 买家确认收货，PENDING_RECEIPT → COMPLETED
        return Result.ok();
    }

    public Result<Void> cancel(Long userId, Long id, String reason) {
        // TODO: C - 取消订单，仅 PENDING_PAYMENT 状态可取消 → CANCELLED
        return Result.ok();
    }

    public Result<PageResult<OrderVO>> getBoughtOrders(Long userId, int page, int size, String status) {
        // TODO: C - 我买的订单列表，按状态筛选
        return Result.ok(PageResult.empty());
    }

    public Result<PageResult<OrderVO>> getSoldOrders(Long userId, int page, int size, String status) {
        // TODO: C - 我卖的订单列表，按状态筛选
        return Result.ok(PageResult.empty());
    }
}
