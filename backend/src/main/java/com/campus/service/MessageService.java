package com.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.Result;
import com.campus.dto.MessageVO;
import com.campus.entity.Message;
import com.campus.entity.Product;
import com.campus.entity.User;
import com.campus.mapper.MessageMapper;
import com.campus.mapper.ProductMapper;
import com.campus.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    public Result<List<MessageVO>> getProductMessages(Long productId) {
        // TODO: C - 查商品下所有留言，构建嵌套结构（parent_id 关联），关联 sender 信息
        List<Message> allMessages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>().eq(Message::getProductId, productId).orderByAsc(Message::getCreatedAt));

        // 1. 获取所有涉及的用户信息，避免N+1查询
        Set<Long> userIds = new HashSet<>();
        allMessages.forEach(m -> { userIds.add(m.getSenderId()); });
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 2. 转换为 VO 并分组
        List<MessageVO> allVOs = allMessages.stream().map(m -> {
            MessageVO vo = new MessageVO();
            BeanUtils.copyProperties(m, vo);
            User sender = userMap.get(m.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }
            vo.setReplies(new ArrayList<>());
            return vo;
        }).collect(Collectors.toList());

        // 3. 手动组装树形结构
        Map<Long, MessageVO> voMap = allVOs.stream().collect(Collectors.toMap(MessageVO::getId, v -> v));
        List<MessageVO> rootMessages = new ArrayList<>();

        for (MessageVO vo : allVOs) {
            Message original = allMessages.stream().filter(m -> m.getId().equals(vo.getId())).findFirst().get();
            if (original.getParentId() == null) {
                rootMessages.add(vo);
            } else {
                MessageVO parent = voMap.get(original.getParentId());
                if (parent != null) parent.getReplies().add(vo);
            }
        }
        return Result.ok(rootMessages);
    }

    public Result<MessageVO> sendMessage(Long senderId, Long productId, String content) {
        // TODO: C - 发布留言，需查商品获取 receiverId(卖家)
        Product product = productMapper.selectById(productId);
        if (product == null) return Result.fail("商品不存在");

        Message message = new Message();
        message.setProductId(productId);
        message.setSenderId(senderId);
        message.setReceiverId(product.getUserId());
        message.setContent(content);
        messageMapper.insert(message);

        return Result.ok(new MessageVO());
    }

    public Result<MessageVO> replyToMessage(Long senderId, Long parentId, String content) {
        // TODO: C - 回复留言，设置 parent_id，receiver_id 为被回复者
        Message parent = messageMapper.selectById(parentId);
        if (parent == null) return Result.fail("原留言不存在");

        Message reply = new Message();
        reply.setProductId(parent.getProductId());
        reply.setSenderId(senderId);
        reply.setReceiverId(parent.getSenderId());
        reply.setParentId(parentId);
        reply.setContent(content);
        messageMapper.insert(reply);

        return Result.ok(new MessageVO());
    }

    public Result<List<MessageVO>> getMyMessages(Long userId) {
        // TODO: C - 获取用户相关的所有私信/留言
        List<Message> list = messageMapper.selectList(
                new LambdaQueryWrapper<Message>().eq(Message::getReceiverId, userId).orderByDesc(Message::getCreatedAt));

        List<MessageVO> vos = list.stream().map(m -> {
            MessageVO vo = new MessageVO();
            BeanUtils.copyProperties(m, vo);
            return vo;
        }).collect(Collectors.toList());

        return Result.ok(vos);
    }
}