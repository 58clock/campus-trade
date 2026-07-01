package com.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.PageResult;
import com.campus.common.Result;
import com.campus.dto.OrderVO;
import com.campus.entity.Order;
import com.campus.entity.Product;
import com.campus.enums.OrderStatus;
import com.campus.enums.ProductStatus;
import com.campus.mapper.OrderMapper;
import com.campus.mapper.ProductMapper;
import com.campus.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    // ==================== [桩] C 实现的真实接口 ====================

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    public Result<OrderVO> createOrder(Long buyerId, Long productId) {
        // TODO: C - 创建订单，校验商品状态，不能自己买自己的，商品状态改为 LOCKED

        Product product = productMapper.selectById(productId);
        if (product == null) return Result.fail("商品不存在");
        if (!product.getStatus().equals(ProductStatus.ON_SALE.name())) return Result.fail("商品已下架或已售出");
        if (product.getUserId().equals(buyerId)) return Result.fail("不能购买自己的商品");

        Long existCount = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getBuyerId, buyerId)
                        .eq(Order::getProductId, productId)
                        .notIn(Order::getStatus, OrderStatus.COMPLETED.name(), OrderStatus.CANCELLED.name()));
        if (existCount > 0) return Result.fail("你已有该商品的未完成订单");

        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setSellerId(product.getUserId());
        order.setProductId(productId);
        order.setAmount(product.getPrice());
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);
        orderMapper.insert(order);

        product.setStatus(ProductStatus.LOCKED.name());
        productMapper.updateById(product);

        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        return Result.ok(vo);
    }

    public Result<OrderVO> getOrderById(Long userId, Long id) {
        // TODO: C - 查询订单详情，校验是买家或卖家
        Order order = orderMapper.selectById(id);
        if (order == null) return Result.fail("订单不存在");
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            return Result.fail("无权查看此订单");
        }
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        return Result.ok(vo);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> pay(Long userId, Long id) {
        // TODO: C - 模拟付款，校验是买家 + 状态是 PENDING_PAYMENT → PENDING_SHIPMENT
        Order order = orderMapper.selectById(id);
        if (order == null) return Result.fail("订单不存在");

        Result<Void> error = checkStatus(order, OrderStatus.PENDING_PAYMENT, true, userId);
        if (error != null) return error;

        order.setStatus(OrderStatus.PENDING_SHIPMENT.name());
        order.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.ok();
    }

    public Result<Void> ship(Long userId, Long id) {
        // TODO: C - 卖家确认发货，PENDING_SHIPMENT → PENDING_RECEIPT
        Order order = orderMapper.selectById(id);
        if (order == null) return Result.fail("订单不存在");

        Result<Void> error = checkStatus(order, OrderStatus.PENDING_SHIPMENT, false, userId);
        if (error != null) return error;

        order.setStatus(OrderStatus.PENDING_RECEIPT.name());
        order.setShippedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.ok();
    }

    public Result<Void> receive(Long userId, Long id) {
        // TODO: C - 买家确认收货，PENDING_RECEIPT → COMPLETED
        Order order = orderMapper.selectById(id);
        if (order == null) return Result.fail("订单不存在");

        Result<Void> error = checkStatus(order, OrderStatus.PENDING_RECEIPT, true, userId);
        if (error != null) return error;

        order.setStatus(OrderStatus.COMPLETED.name());
        order.setReceivedAt(LocalDateTime.now());
        order.setCompletedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.ok();
    }

    public Result<Void> cancel(Long userId, Long id, String reason) {
        // TODO: C - 取消订单，仅 PENDING_PAYMENT 状态可取消 → CANCELLED
        Order order = orderMapper.selectById(id);
        if (order == null) return Result.fail("订单不存在");

        if (!order.getBuyerId().equals(userId)) return Result.fail("无权操作");
        if (!order.getStatus().equals(OrderStatus.PENDING_PAYMENT.name()) &&
                !order.getStatus().equals(OrderStatus.PENDING_SHIPMENT.name())) {
            return Result.fail("当前订单无法取消");
        }

        order.setStatus(OrderStatus.CANCELLED.name());
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(reason);
        orderMapper.updateById(order);

        Product product = productMapper.selectById(order.getProductId());
        if (product != null) {
            product.setStatus(ProductStatus.ON_SALE.name());
            productMapper.updateById(product);
        }
        return Result.ok();
    }

    public Result<PageResult<OrderVO>> getBoughtOrders(Long userId, int page, int size, String status) {
        // TODO: C - 我买的订单列表，按状态筛选
        Page<Order> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getBuyerId, userId);
        if (StringUtils.hasText(status)) wrapper.eq(Order::getStatus, status);

        Page<Order> orderPage = orderMapper.selectPage(pageParam, wrapper);
        List<OrderVO> voList = orderPage.getRecords().stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            return vo;
        }).collect(Collectors.toList());

        return Result.ok(new PageResult<>(voList, orderPage.getTotal(), orderPage.getCurrent(), orderPage.getSize()));
    }


    public Result<PageResult<OrderVO>> getSoldOrders(Long userId, int page, int size, String status) {
        // TODO: C - 我卖的订单列表，按状态筛选
        Page<Order> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getSellerId, userId);
        if (StringUtils.hasText(status)) wrapper.eq(Order::getStatus, status);
        wrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> orderPage = orderMapper.selectPage(pageParam, wrapper);
        List<OrderVO> voList = orderPage.getRecords().stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            return vo;
        }).collect(Collectors.toList());

        return Result.ok(new PageResult<>(voList, orderPage.getTotal(), orderPage.getCurrent(), orderPage.getSize()));
    }

    private Result<Void> checkStatus(Order order, OrderStatus expected, boolean isBuyer, Long userId) {
        if (!order.getStatus().equals(expected.name())) return Result.fail("订单状态不正确");
        if (isBuyer && !order.getBuyerId().equals(userId)) return Result.fail("只有买家才能操作");
        if (!isBuyer && !order.getSellerId().equals(userId)) return Result.fail("只有卖家才能操作");
        return null;
    }
}

