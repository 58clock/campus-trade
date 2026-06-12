package com.campus.service;

import com.campus.common.Result;
import com.campus.dto.MessageVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    // ==================== [桩] C 实现的真实接口 ====================

    public Result<List<MessageVO>> getProductMessages(Long productId) {
        // TODO: C - 查商品下所有留言，构建嵌套结构（parent_id 关联），关联 sender 信息
        return Result.ok(List.of());
    }

    public Result<MessageVO> sendMessage(Long senderId, Long productId, String content) {
        // TODO: C - 发布留言，需查商品获取 receiverId(卖家)
        return Result.ok(new MessageVO());
    }

    public Result<MessageVO> replyToMessage(Long senderId, Long parentId, String content) {
        // TODO: C - 回复留言，设置 parent_id，receiver_id 为被回复者
        return Result.ok(new MessageVO());
    }

    public Result<List<MessageVO>> getMyMessages(Long userId) {
        // TODO: C - 获取用户相关的所有私信/留言
        return Result.ok(List.of());
    }
}
