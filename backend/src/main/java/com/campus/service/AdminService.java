package com.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.PageResult;
import com.campus.common.Result;
import com.campus.dto.*;
import com.campus.entity.*;
import com.campus.enums.OrderStatus;
import com.campus.enums.ProductCategory;
import com.campus.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final ReportMapper reportMapper;
    private final ReviewMapper reviewMapper;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // ==================== [桩] D 实现的真实接口 ====================

    public Result<PageResult<UserVO>> listUsers(int pageNum, int size, String keyword, Integer status) {
        // TODO: D - 用户列表（搜索、筛选、分页）
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword));
        }
        if (status != null) wrapper.eq(User::getStatus, status);
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> page = new Page<>(pageNum, size);
        Page<User> result = userMapper.selectPage(page, wrapper);

        List<UserVO> records = result.getRecords().stream().map(u -> {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setEmail(u.getEmail());
            vo.setPhone(u.getPhone());
            vo.setNickname(u.getNickname());
            vo.setSchool(u.getSchool());
            vo.setAvatar(u.getAvatar());
            vo.setRole(u.getRole());
            vo.setStatus(u.getStatus());
            vo.setCreatedAt(u.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());

        return Result.ok(new PageResult<>(records, result.getTotal(), pageNum, size));
    }

    public Result<Void> banUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) return Result.fail("用户不存在");
        user.setStatus(0);
        userMapper.updateById(user);
        return Result.ok();
    }

    public Result<Void> unbanUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) return Result.fail("用户不存在");
        user.setStatus(1);
        userMapper.updateById(user);
        return Result.ok();
    }

    public Result<Void> resetPassword(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) return Result.fail("用户不存在");
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
        return Result.ok();
    }

    public Result<PageResult<ProductVO>> listAllProducts(int pageNum, int size, String keyword, String status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Product::getTitle, keyword);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getCreatedAt);

        Page<Product> page = new Page<>(pageNum, size);
        Page<Product> result = productMapper.selectPage(page, wrapper);

        List<ProductVO> records = result.getRecords().stream().map(p -> {
            ProductVO vo = new ProductVO();
            vo.setId(p.getId());
            vo.setUserId(p.getUserId());
            vo.setTitle(p.getTitle());
            vo.setDescription(p.getDescription());
            vo.setCategory(p.getCategory());
            vo.setPrice(p.getPrice());
            vo.setOriginalPrice(p.getOriginalPrice());
            vo.setConditionLevel(p.getConditionLevel());
            vo.setStatus(p.getStatus());
            vo.setViewCount(p.getViewCount());
            vo.setCreatedAt(p.getCreatedAt());
            User seller = userMapper.selectById(p.getUserId());
            if (seller != null) {
                vo.setSellerName(seller.getNickname());
                vo.setSellerAvatar(seller.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());

        return Result.ok(new PageResult<>(records, result.getTotal(), pageNum, size));
    }

    public Result<Void> forceOffShelf(Long id) {
        // TODO: D - 管理员强制下架
        Product p = productMapper.selectById(id);
        if (p == null) return Result.fail("商品不存在");
        p.setStatus("OFF_SHELF");
        productMapper.updateById(p);
        return Result.ok();
    }

    public Result<Void> deleteProduct(Long id) {
        // TODO: D - 管理员删除商品
        Product p = productMapper.selectById(id);
        if (p == null) return Result.fail("商品不存在");
        p.setStatus("DELETED");
        productMapper.updateById(p);
        return Result.ok();
    }

    public Result<PageResult<ReportVO>> listReports(int pageNum, int size, String status) {
        // TODO: D - 举报列表，按状态筛选
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Report::getStatus, status);
        wrapper.orderByDesc(Report::getCreatedAt);

        Page<Report> page = new Page<>(pageNum, size);
        Page<Report> result = reportMapper.selectPage(page, wrapper);

        List<ReportVO> records = result.getRecords().stream().map(r -> {
            ReportVO vo = new ReportVO();
            vo.setId(r.getId());
            vo.setReporterId(r.getReporterId());
            vo.setTargetType(r.getTargetType());
            vo.setTargetId(r.getTargetId());
            vo.setReason(r.getReason());
            vo.setStatus(r.getStatus());
            vo.setHandlerNote(r.getHandlerNote());
            vo.setCreatedAt(r.getCreatedAt());
            User reporter = userMapper.selectById(r.getReporterId());
            if (reporter != null) vo.setReporterName(reporter.getUsername());
            return vo;
        }).collect(Collectors.toList());

        return Result.ok(new PageResult<>(records, result.getTotal(), pageNum, size));
    }

    public Result<Void> handleReport(Long id, String action, String note) {
        Report report = reportMapper.selectById(id);
        if (report == null) return Result.fail("举报不存在");
        if ("resolve".equals(action)) {
            report.setStatus("RESOLVED");
            if ("PRODUCT".equals(report.getTargetType())) {
                Product p = productMapper.selectById(report.getTargetId());
                if (p != null && "ON_SALE".equals(p.getStatus())) {
                    p.setStatus("OFF_SHELF");
                    productMapper.updateById(p);
                }
            }
        } else if ("dismiss".equals(action)) {
            report.setStatus("DISMISSED");
        } else {
            return Result.fail("无效的操作");
        }
        report.setHandlerNote(note);
        reportMapper.updateById(report);
        return Result.ok();
    }

    public Result<StatisticsVO> getStatistics() {
        // TODO: D - 聚合查询，返回 ECharts 前端所需数据
        StatisticsVO vo = new StatisticsVO();

        vo.setTotalProducts(productMapper.selectCount(null));
        vo.setTotalOrders(orderMapper.selectCount(null));
        vo.setTotalUsers(userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, "USER")));

        // 总交易额
        List<Order> paidOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().ne(Order::getStatus, "CANCELLED"));
        BigDecimal totalSales = paidOrders.stream()
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalSales(totalSales);

        // 近一周每日交易额
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(6);
        List<Order> recentOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .ne(Order::getStatus, "CANCELLED")
                        .ge(Order::getPaidAt, sevenDaysAgo.atStartOfDay()));

        Map<LocalDate, BigDecimal> dayMap = recentOrders.stream()
                .filter(o -> o.getPaidAt() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getPaidAt().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Order::getAmount, BigDecimal::add)));

        List<StatisticsVO.DailySales> weekly = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            StatisticsVO.DailySales ds = new StatisticsVO.DailySales();
            ds.setDay(day.format(DateTimeFormatter.ofPattern("MM-dd")));
            ds.setTotal(dayMap.getOrDefault(day, BigDecimal.ZERO));
            weekly.add(ds);
        }
        vo.setWeeklySales(weekly);

        // 分类分布
        Map<String, Long> categoryDist = new HashMap<>();
        for (ProductCategory cat : ProductCategory.values()) {
            categoryDist.put(cat.name(), productMapper.selectCount(
                    new LambdaQueryWrapper<Product>().eq(Product::getCategory, cat.name())));
        }
        vo.setCategoryDistribution(categoryDist);

        // 订单状态分布
        Map<String, Long> statusDist = new HashMap<>();
        for (OrderStatus st : OrderStatus.values()) {
            statusDist.put(st.name(), orderMapper.selectCount(
                    new LambdaQueryWrapper<Order>().eq(Order::getStatus, st.name())));
        }
        vo.setOrderStatusDistribution(statusDist);

        return Result.ok(vo);
    }

    // ==================== 订单管理 ====================

    public Result<PageResult<OrderVO>> listAllOrders(int pageNum, int size, String keyword, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> page = new Page<>(pageNum, size);
        Page<Order> result = orderMapper.selectPage(page, wrapper);

        List<OrderVO> records = result.getRecords().stream().map(o -> {
            OrderVO vo = new OrderVO();
            vo.setId(o.getId());
            vo.setProductId(o.getProductId());
            vo.setBuyerId(o.getBuyerId());
            vo.setSellerId(o.getSellerId());
            vo.setAmount(o.getAmount());
            vo.setStatus(o.getStatus());
            vo.setPaidAt(o.getPaidAt());
            vo.setShippedAt(o.getShippedAt());
            vo.setReceivedAt(o.getReceivedAt());
            vo.setCompletedAt(o.getCompletedAt());
            vo.setCancelledAt(o.getCancelledAt());
            vo.setCancelReason(o.getCancelReason());
            vo.setCreatedAt(o.getCreatedAt());

            User buyer = userMapper.selectById(o.getBuyerId());
            if (buyer != null) vo.setBuyerName(buyer.getNickname());

            User seller = userMapper.selectById(o.getSellerId());
            if (seller != null) vo.setSellerName(seller.getNickname());

            Product product = productMapper.selectById(o.getProductId());
            if (product != null) vo.setProductTitle(product.getTitle());

            return vo;
        }).collect(Collectors.toList());

        // 按商品标题过滤
        if (keyword != null && !keyword.isBlank()) {
            records = records.stream()
                    .filter(vo -> vo.getProductTitle() != null
                            && vo.getProductTitle().contains(keyword))
                    .collect(Collectors.toList());
        }

        return Result.ok(new PageResult<>(records, result.getTotal(), pageNum, size));
    }

    public Result<Void> cancelOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) return Result.fail("订单不存在");
        if ("CANCELLED".equals(order.getStatus())) return Result.fail("订单已取消");
        order.setStatus("CANCELLED");
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason("管理员取消");
        orderMapper.updateById(order);
        return Result.ok();
    }
}
