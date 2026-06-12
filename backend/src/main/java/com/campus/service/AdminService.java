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
        // TODO: D - 重置为默认密码 123456
        User user = userMapper.selectById(id);
        if (user == null) return Result.fail("用户不存在");
        return Result.ok();
    }

    public Result<PageResult<ProductVO>> listAllProducts(int pageNum, int size, String keyword, String status) {
        // TODO: D - 全部商品（含下架和删除），审核用
        return Result.ok(PageResult.empty());
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
        // TODO: D - 处理举报, action: resolve/dismiss
        Report report = reportMapper.selectById(id);
        if (report == null) return Result.fail("举报不存在");
        if ("resolve".equals(action)) {
            report.setStatus("RESOLVED");
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
        List<StatisticsVO.DailySales> weekly = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            StatisticsVO.DailySales ds = new StatisticsVO.DailySales();
            ds.setDay(day.format(DateTimeFormatter.ofPattern("MM-dd")));
            ds.setTotal(BigDecimal.ZERO);
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
}
