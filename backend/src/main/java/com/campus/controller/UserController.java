package com.campus.controller;

import com.campus.common.Result;
import com.campus.dto.UpdateProfileRequest;
import com.campus.dto.UserVO;
import com.campus.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户", description = "个人信息管理接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/profile")
    public Result<UserVO> getProfile(Authentication auth) {
        return userService.getProfile(getUserId(auth));
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(Authentication auth,
                                         @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(getUserId(auth), request);
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(Authentication auth,
                                        @RequestParam("file") MultipartFile file) {
        return userService.uploadAvatar(getUserId(auth), file);
    }

    @Operation(summary = "获取用户公开信息")
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
