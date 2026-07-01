package com.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.Result;
import com.campus.dto.*;
import com.campus.entity.User;
import com.campus.mapper.UserMapper;
import com.campus.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_LOGIN_FAILS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);

    public Result<Void> register(RegisterRequest request) {
        if (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())) > 0) {
            return Result.fail("用户名已存在");
        }
        if (request.getEmail() != null && userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())) > 0) {
            return Result.fail("邮箱已被注册");
        }
        if (request.getPhone() != null && userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())) > 0) {
            return Result.fail("手机号已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(request.getUsername());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);

        return Result.ok();
    }

    public Result<LoginResponse> login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            return Result.fail("用户名或密码错误");
        }

        // 检查是否被封禁
        if (user.getStatus() == 0) {
            return Result.fail("账号已被封禁");
        }

        // 检查是否被冻结
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            return Result.fail("账号已被冻结，请" + user.getLockedUntil().toLocalTime() + "后重试");
        }

        // 校验密码
//        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        if (!request.getPassword().equals(user.getPassword())) {
//            String failKey = "login_fail:" + user.getId();
            String failKey = "login_fail:" + user.getId();
            Long failCount = redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, LOCK_DURATION);

            if (failCount != null && failCount >= MAX_LOGIN_FAILS) {
                user.setLockedUntil(LocalDateTime.now().plus(LOCK_DURATION));
                user.setLoginFailCount(0);
                userMapper.updateById(user);
                return Result.fail("密码错误次数过多，账号已冻结30分钟");
            }
            user.setLoginFailCount(failCount != null ? failCount.intValue() : 0);
            userMapper.updateById(user);
            return Result.fail("用户名或密码错误，剩余尝试次数: " + (MAX_LOGIN_FAILS - (failCount != null ? failCount : 0)));
        }

        // 登录成功，清除失败记录
        redisTemplate.delete("login_fail:" + user.getId());
        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        userMapper.updateById(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole());

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

        return Result.ok(new LoginResponse(token, vo));
    }

    public Result<Void> sendResetCode(String email) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            return Result.fail("该邮箱未注册");
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        redisTemplate.opsForValue().set("reset_code:" + email, code, Duration.ofMinutes(5));

        // 模拟发送邮件：打印到控制台
        System.out.println("==========================================");
        System.out.println("  密码重置验证码: " + code);
        System.out.println("  收件人: " + email);
        System.out.println("  有效期: 5分钟");
        System.out.println("==========================================");

        return Result.ok();
    }

    public Result<Void> resetPassword(ForgotPasswordRequest request) {
        String storedCode = (String) redisTemplate.opsForValue()
                .get("reset_code:" + request.getEmail());
        if (storedCode == null || !storedCode.equals(request.getCode())) {
            return Result.fail("验证码错误或已过期");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);

        redisTemplate.delete("reset_code:" + request.getEmail());
        return Result.ok();
    }
}
