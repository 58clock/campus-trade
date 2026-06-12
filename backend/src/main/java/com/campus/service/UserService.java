package com.campus.service;

import com.campus.common.Result;
import com.campus.dto.UpdateProfileRequest;
import com.campus.dto.UserVO;
import com.campus.entity.User;
import com.campus.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    public Result<UserVO> getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        return Result.ok(toVO(user));
    }

    public Result<UserVO> updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getSchool() != null) user.setSchool(request.getSchool());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        userMapper.updateById(user);
        return Result.ok(toVO(user));
    }

    public Result<String> uploadAvatar(Long userId, MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String ext = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

            Path avatarDir = Paths.get(uploadPath, "avatar");
            Files.createDirectories(avatarDir);
            Path filepath = avatarDir.resolve(filename);
            file.transferTo(filepath.toFile());

            String avatarUrl = "/uploads/avatar/" + filename;
            User user = userMapper.selectById(userId);
            user.setAvatar(avatarUrl);
            userMapper.updateById(user);

            return Result.ok(avatarUrl);
        } catch (IOException e) {
            return Result.fail("头像上传失败: " + e.getMessage());
        }
    }

    public Result<UserVO> getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) return Result.fail("用户不存在");
        return Result.ok(toVO(user));
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setSchool(user.getSchool());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
