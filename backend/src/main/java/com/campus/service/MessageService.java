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
        // 1. 获取留言列表
        List<Message> allMessages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>().eq(Message::getProductId, productId).orderByAsc(Message::getCreatedAt));
        if (allMessages.isEmpty()) return Result.ok(new ArrayList<>());

        // 2. 获取用户并预处理 VO
        Set<Long> userIds = allMessages.stream().map(Message::getSenderId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 3. 一次遍历：转换 VO 并放入 Map 便于查找父节点
        Map<Long, MessageVO> voMap = new LinkedHashMap<>();
        for (Message m : allMessages) {
            MessageVO vo = new MessageVO();
            BeanUtils.copyProperties(m, vo);
            User sender = userMap.get(m.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }
            vo.setReplies(new ArrayList<>());
            voMap.put(vo.getId(), vo);
        }

        // 4. 一次遍历：构建树
        List<MessageVO> rootMessages = new ArrayList<>();
        for (Message m : allMessages) {
            MessageVO vo = voMap.get(m.getId());
            if (m.getParentId() == null || m.getParentId() == 0) { // 建议加上 == 0 的判断，防止数据库为0的情况
                rootMessages.add(vo);
            } else {
                MessageVO parent = voMap.get(m.getParentId());
                if (parent != null) {
                    parent.getReplies().add(vo);
                } else {
                    // 如果父节点不存在（比如父留言被删了），可以视作顶级留言或者跳过
                    rootMessages.add(vo);
                }
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
        message.setCreatedAt(java.time.LocalDateTime.now());
        messageMapper.insert(message);

        // 转换为 VO 并填充用户信息，实现“实名”返回
        // 转换为 VO 并填充用户信息，实现“实名”返回
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);

        // --- 关键修改点 1：显式初始化 replies，防止返回 null ---
        vo.setReplies(new ArrayList<>());

        // 获取当前发送者信息并设置
        User sender = userMapper.selectById(senderId);
        if (sender != null) {
            vo.setSenderName(sender.getNickname());
            vo.setSenderAvatar(sender.getAvatar());
        } else {
            // --- 关键修改点 2：给一个兜底值，防止前端显示 null ---
            vo.setSenderName("匿名用户");
        }

        return Result.ok(vo);
    }

    public Result<MessageVO> replyToMessage(Long senderId, Long parentId, String content) {
        // TODO: C - 回复留言，设置 parent_id，receiver_id 为被回复者
        // 1. 查询原留言
        Message parent = messageMapper.selectById(parentId);
        if (parent == null) return Result.fail("原留言不存在");

        // 2. 构造回复对象
        Message reply = new Message();
        reply.setProductId(parent.getProductId());
        reply.setSenderId(senderId);
        reply.setReceiverId(parent.getSenderId());
        reply.setParentId(parentId);
        reply.setContent(content);
        reply.setCreatedAt(java.time.LocalDateTime.now());

        // 3. 插入数据库
        messageMapper.insert(reply);

        // 4. 封装 VO，包含发送者用户信息以实现“实名”
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(reply, vo);

        // 获取当前回复者信息
        User sender = userMapper.selectById(senderId);
        if (sender != null) {
            vo.setSenderName(sender.getNickname());
            vo.setSenderAvatar(sender.getAvatar());
        }

        return Result.ok(vo);
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