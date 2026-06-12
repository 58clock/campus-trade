package com.campus.service;

import com.campus.common.PageResult;
import com.campus.common.Result;
import com.campus.dto.ReviewRequest;
import com.campus.dto.ReviewVO;
import com.campus.dto.UserVO;
import com.campus.entity.Review;
import com.campus.entity.Order;
import com.campus.entity.User;
import com.campus.mapper.ReviewMapper;
import com.campus.mapper.OrderMapper;
import com.campus.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    public Result<Void> createReview(Long reviewerId, ReviewRequest request) {
        // TODO: C - 校验订单状态为 COMPLETED，互评双方各一次
        // 更新用户信誉分

        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) return Result.fail("订单不存在");
        if (!"COMPLETED".equals(order.getStatus())) return Result.fail("订单未完成，无法评价");

        Long targetId;
        if (reviewerId.equals(order.getBuyerId())) {
            targetId = order.getSellerId();
        } else if (reviewerId.equals(order.getSellerId())) {
            targetId = order.getBuyerId();
        } else {
            return Result.fail("无权评价");
        }

        // 检查是否已评价
        Long count = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, request.getOrderId())
                .eq(Review::getReviewerId, reviewerId));
        if (count > 0) return Result.fail("已评价过此订单");

        Review review = new Review();
        review.setOrderId(request.getOrderId());
        review.setReviewerId(reviewerId);
        review.setTargetId(targetId);
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setStatus("SHOW");
        reviewMapper.insert(review);

        return Result.ok();
    }

    public Result<PageResult<ReviewVO>> getUserReviews(Long userId, int pageNum, int size) {
        Page<Review> page = new Page<>(pageNum, size);
        Page<Review> result = reviewMapper.selectPage(page,
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getTargetId, userId)
                        .eq(Review::getStatus, "SHOW")
                        .orderByDesc(Review::getCreatedAt));

        PageResult<ReviewVO> pageResult = new PageResult<>(
                result.getRecords().stream().map(this::toVO).collect(Collectors.toList()),
                result.getTotal(), pageNum, size);
        return Result.ok(pageResult);
    }

    public Result<UserVO> getUserReputation(Long userId) {
        // TODO: C - 计算信誉分 = 总星数 / 评价数
        User user = userMapper.selectById(userId);
        if (user == null) return Result.fail("用户不存在");

        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setSchool(user.getSchool());

        // 计算平均评分
        Double avgRating = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getTargetId, userId))
                .stream().mapToInt(Review::getRating).average().orElse(0.0);
        vo.setReputationScore(avgRating);

        return Result.ok(vo);
    }

    private ReviewVO toVO(Review r) {
        ReviewVO vo = new ReviewVO();
        vo.setId(r.getId());
        vo.setOrderId(r.getOrderId());
        vo.setReviewerId(r.getReviewerId());
        vo.setRating(r.getRating());
        vo.setContent(r.getContent());
        vo.setStatus(r.getStatus());
        vo.setCreatedAt(r.getCreatedAt());

        User reviewer = userMapper.selectById(r.getReviewerId());
        if (reviewer != null) {
            vo.setReviewerName(reviewer.getNickname());
            vo.setReviewerAvatar(reviewer.getAvatar());
        }
        return vo;
    }
}
