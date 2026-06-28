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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    public Result<List<MessageVO>> getProductMessages(Long productId) {
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getProductId, productId)
                        .orderByAsc(Message::getCreatedAt));

        List<MessageVO> all = messages.stream().map(this::toVO).collect(Collectors.toList());

        // 构建 parentId -> replies 映射
        Map<Long, List<MessageVO>> repliesMap = new java.util.HashMap<>();
        List<MessageVO> roots = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            MessageVO vo = all.get(i);
            if (msg.getParentId() == null) {
                roots.add(vo);
            } else {
                repliesMap.computeIfAbsent(msg.getParentId(), k -> new ArrayList<>()).add(vo);
            }
        }

        for (MessageVO root : roots) {
            root.setReplies(repliesMap.getOrDefault(root.getId(), new ArrayList<>()));
        }

        return Result.ok(roots);
    }

    public Result<MessageVO> sendMessage(Long senderId, Long productId, String content) {
        Product product = productMapper.selectById(productId);
        if (product == null) return Result.fail("商品不存在");

        Message msg = new Message();
        msg.setProductId(productId);
        msg.setSenderId(senderId);
        msg.setReceiverId(product.getUserId());
        msg.setContent(content);
        messageMapper.insert(msg);

        return Result.ok(toVO(msg));
    }

    public Result<MessageVO> replyToMessage(Long senderId, Long parentId, String content) {
        Message parent = messageMapper.selectById(parentId);
        if (parent == null) return Result.fail("原留言不存在");

        Message msg = new Message();
        msg.setProductId(parent.getProductId());
        msg.setSenderId(senderId);
        msg.setReceiverId(parent.getSenderId());
        msg.setParentId(parentId);
        msg.setContent(content);
        messageMapper.insert(msg);

        return Result.ok(toVO(msg));
    }

    public Result<List<MessageVO>> getMyMessages(Long userId) {
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getReceiverId, userId)
                        .orderByDesc(Message::getCreatedAt));

        return Result.ok(messages.stream().map(this::toVO).collect(Collectors.toList()));
    }

    private MessageVO toVO(Message msg) {
        MessageVO vo = new MessageVO();
        vo.setId(msg.getId());
        vo.setSenderId(msg.getSenderId());
        vo.setReceiverId(msg.getReceiverId());
        vo.setContent(msg.getContent());
        vo.setCreatedAt(msg.getCreatedAt());
        User sender = userMapper.selectById(msg.getSenderId());
        if (sender != null) {
            vo.setSenderName(sender.getNickname());
            vo.setSenderAvatar(sender.getAvatar());
        }
        return vo;
    }
}
