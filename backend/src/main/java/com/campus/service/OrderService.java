package com.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.PageResult;
import com.campus.common.Result;
import com.campus.dto.OrderVO;
import com.campus.entity.Order;
import com.campus.entity.Product;
import com.campus.mapper.OrderMapper;
import com.campus.mapper.ProductMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    // ==================== [桩] C 实现的真实接口 ====================

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Transactional(rollbackFor = Exception.class)
    public Result<OrderVO> createOrder(Long buyerId, Long productId) {
        // TODO: C - 创建订单，校验商品状态，不能自己买自己的，商品状态改为 LOCKED
        // 初始状态 PENDING_PAYMENT
    // 1. 校验商品存在
        Product product = productMapper.selectById(productId);
        if (product == null) return Result.fail("商品不存在");

        // 2. 校验权限
        if (product.getUserId().equals(buyerId)) return Result.fail("不能购买自己的商品");

        // 3. 校验状态 (假设 AVAILABLE 是可购买)
        if (!"AVAILABLE".equals(product.getStatus())) return Result.fail("商品已售出或锁定");

        // 4. 锁定商品
        product.setStatus("LOCKED");
        productMapper.updateById(product);

        // 5. 生成订单
        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setSellerId(product.getUserId());
        order.setProductId(productId);
        order.setAmount(product.getPrice());
        order.setStatus("PENDING_PAYMENT");
        order.setCreatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        // 6. 返回结果
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo); // 注意：这行需要你有一个 OrderVO 类
        return Result.ok(vo);
    }

    public Result<OrderVO> getOrderById(Long userId, Long id) {
        // TODO: C - 查询订单详情，校验是买家或卖家
        return Result.ok(new OrderVO());
    }
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> pay(Long userId, Long id) {
        // TODO: C - 模拟付款，校验是买家 + 状态是 PENDING_PAYMENT → PENDING_SHIPMENT
        // 1. 获取订单
        Order order = orderMapper.selectById(id);
        if (order == null) return Result.fail("订单不存在");

        // 2. 权限校验：只能支付自己的订单
        if (!order.getBuyerId().equals(userId)) return Result.fail("无权操作此订单");

        // 3. 状态校验：只有 PENDING_PAYMENT 才能支付
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            return Result.fail("订单状态异常，无法支付");
        }

        // 4. 更新状态
        order.setStatus("PENDING_SHIPMENT");
        order.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(order);

        return Result.ok();
    }

    public Result<Void> ship(Long userId, Long id) {
        // TODO: C - 卖家确认发货，PENDING_SHIPMENT → PENDING_RECEIPT
        // 1. 获取订单
        Order order = orderMapper.selectById(id);
        if (order == null) return Result.fail("订单不存在");

        // 2. 权限校验：必须是卖家才能发货
        if (!order.getSellerId().equals(userId)) return Result.fail("无权操作此订单");

        // 3. 状态校验：只有已付款的订单才能发货
        if (!"PENDING_SHIPMENT".equals(order.getStatus())) {
            return Result.fail("当前订单状态无法发货");
        }

        // 4. 更新状态：待收货
        order.setStatus("PENDING_RECEIPT");
        order.setShippedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.ok();
    }

    public Result<Void> receive(Long userId, Long id) {
        // TODO: C - 买家确认收货，PENDING_RECEIPT → COMPLETED
        // 1. 获取订单
        Order order = orderMapper.selectById(id);
        if (order == null) return Result.fail("订单不存在");

        // 2. 权限校验：必须是买家才能确认收货
        if (!order.getBuyerId().equals(userId)) return Result.fail("无权操作此订单");

        // 3. 状态校验：只有待收货的订单才能确认收货
        if (!"PENDING_RECEIPT".equals(order.getStatus())) {
            return Result.fail("当前订单无法确认收货");
        }

        // 4. 更新状态：订单完成
        order.setStatus("COMPLETED");
        order.setReceivedAt(LocalDateTime.now());
        order.setCompletedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.ok();
    }

    public Result<Void> cancel(Long userId, Long id, String reason) {
        // TODO: C - 取消订单，仅 PENDING_PAYMENT 状态可取消 → CANCELLED
        // 1. 获取订单
        Order order = orderMapper.selectById(id);
        if (order == null) return Result.fail("订单不存在");

        // 2. 校验权限：只能取消自己的订单
        if (!order.getBuyerId().equals(userId)) return Result.fail("无权操作此订单");

        // 3. 状态校验：只有 PENDING_PAYMENT 状态的订单才能取消
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            return Result.fail("订单状态已无法取消");
        }

        // 4. 修改状态为 CANCELLED
        order.setStatus("CANCELLED");
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(reason);
        orderMapper.updateById(order);

        // 5. 关键动作：要把商品的锁定状态释放掉！
        // 订单取消了，商品就应该变回 AVAILABLE，让别人能买
        Product product = productMapper.selectById(order.getProductId());
        if (product != null) {
            product.setStatus("AVAILABLE");
            productMapper.updateById(product);
        }

        return Result.ok();
    }

    public Result<PageResult<OrderVO>> getBoughtOrders(Long userId, int page, int size, String status) {
        // TODO: C - 我买的订单列表，按状态筛选
        // 1. MyBatis-Plus 的分页插件（这是 MP 自带的工具，不是实体类的属性）
        Page<Order> pageParam = new Page<>(page, size);

        // 2. 查询条件
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getBuyerId, userId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Order::getStatus, status);
        }

        // 3. 执行查询，得到的是 MP 的 Page 对象
        Page<Order> orderPage = orderMapper.selectPage(pageParam, wrapper);

        // 4. 手动转换：把 Order 转换成 OrderVO
        List<OrderVO> voList = orderPage.getRecords().stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            // 如果 OrderVO 需要更多信息（如商品名），在这里补充查询
            return vo;
        }).collect(Collectors.toList());

        // 5. 封装进你项目中定义的 PageResult
        PageResult<OrderVO> result = new PageResult<>(
                voList,
                orderPage.getTotal(),
                orderPage.getCurrent(),
                orderPage.getSize()
        );
        return Result.ok(result);
    }

    public Result<PageResult<OrderVO>> getSoldOrders(Long userId, int page, int size, String status) {
        // TODO: C - 我卖的订单列表，按状态筛选
        // 1. 创建分页对象
        Page<Order> pageParam = new Page<>(page, size);

        // 2. 构建查询条件：筛选 sellerId 而不是 buyerId
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getSellerId, userId);

        // 如果传入了状态，就加上筛选
        if (StringUtils.hasText(status)) {
            wrapper.eq(Order::getStatus, status);
        }

        // 按时间倒序
        wrapper.orderByDesc(Order::getCreatedAt);

        // 3. 执行查询
        Page<Order> orderPage = orderMapper.selectPage(pageParam, wrapper);

        // 4. 数据转换：将 Entity 转为 VO
        List<OrderVO> voList = orderPage.getRecords().stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            return vo;
        }).collect(Collectors.toList());

        // 5. 封装结果
        PageResult<OrderVO> result = new PageResult<>(
                voList,
                orderPage.getTotal(),
                orderPage.getCurrent(),
                orderPage.getSize()
        );

        return Result.ok(result);
    }
}
