package com.campus.controller;

import com.campus.common.Result;
import com.campus.entity.Report;
import com.campus.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "举报", description = "用户举报接口")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "提交举报")
    @PostMapping
    public Result<Void> submitReport(Authentication auth,
                                      @RequestParam String targetType,
                                      @RequestParam Long targetId,
                                      @RequestParam String reason) {
        return reportService.submitReport(getUserId(auth), targetType, targetId, reason);
    }

    @Operation(summary = "我的举报记录")
    @GetMapping("/my")
    public Result<List<Report>> getMyReports(Authentication auth) {
        return reportService.getMyReports(getUserId(auth));
    }

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
