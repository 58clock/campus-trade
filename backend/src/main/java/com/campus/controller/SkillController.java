package com.campus.controller;

import com.campus.common.Result;
import com.campus.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Skill", description = "开放Skill接口（智能推荐/自动定价）")
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @Operation(summary = "智能推荐", description = "根据用户浏览历史推荐相似商品")
    @PostMapping("/recommend")
    public Result<List<Map<String, Object>>> recommend(Authentication auth,
                                                        @RequestParam(defaultValue = "10") int limit) {
        return skillService.recommend(getUserId(auth), limit);
    }

    @Operation(summary = "自动定价", description = "基于分类、成色、市场均价给出建议售价")
    @PostMapping("/pricing")
    public Result<Map<String, Object>> suggestPrice(@RequestParam String category,
                                                     @RequestParam String conditionLevel) {
        return skillService.suggestPrice(category, conditionLevel);
    }

    private Long getUserId(Authentication auth) {
        return auth != null ? (Long) auth.getPrincipal() : null;
    }
}
