package com.campus.controller;

import com.campus.common.PageResult;
import com.campus.common.Result;
import com.campus.dto.ReviewRequest;
import com.campus.dto.UserVO;
import com.campus.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "评价", description = "评价与信誉体系接口")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "发表评价")
    @PostMapping
    public Result<Void> createReview(Authentication auth,
                                      @Valid @RequestBody ReviewRequest request) {
        return reviewService.createReview(getUserId(auth), request);
    }

    @Operation(summary = "用户评价列表")
    @GetMapping("/user/{userId}")
    public Result<PageResult<com.campus.dto.ReviewVO>> getUserReviews(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return reviewService.getUserReviews(userId, page, size);
    }

    @Operation(summary = "用户信誉信息")
    @GetMapping("/user/{userId}/reputation")
    public Result<UserVO> getUserReputation(@PathVariable Long userId) {
        return reviewService.getUserReputation(userId);
    }

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
