package com.campus.controller;

import com.campus.common.Result;
import com.campus.dto.MessageVO;
import com.campus.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "留言", description = "商品留言/私信接口")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "商品留言列表(公开)")
    @GetMapping("/products/{productId}/messages")
    public Result<List<MessageVO>> getProductMessages(@PathVariable Long productId) {
        return messageService.getProductMessages(productId);
    }

    @Operation(summary = "发布留言")
    @PostMapping("/products/{productId}/messages")
    public Result<MessageVO> sendMessage(Authentication auth,
                                          @PathVariable Long productId,
                                          @RequestParam String content) {
        return messageService.sendMessage(getUserId(auth), productId, content);
    }

    @Operation(summary = "回复留言")
    @PostMapping("/messages/{id}/reply")
    public Result<MessageVO> replyToMessage(Authentication auth,
                                             @PathVariable Long id,
                                             @RequestParam String content) {
        return messageService.replyToMessage(getUserId(auth), id, content);
    }

    @Operation(summary = "我的私信列表")
    @GetMapping("/messages")
    public Result<List<MessageVO>> myMessages(Authentication auth) {
        return messageService.getMyMessages(getUserId(auth));
    }

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
