package com.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.Result;
import com.campus.entity.Report;
import com.campus.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;

    public Result<Void> submitReport(Long reporterId, String targetType, Long targetId, String reason) {
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setStatus("PENDING");
        reportMapper.insert(report);
        return Result.ok();
    }

    public Result<List<Report>> getMyReports(Long userId) {
        List<Report> reports = reportMapper.selectList(
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getReporterId, userId)
                        .orderByDesc(Report::getCreatedAt));
        return Result.ok(reports);
    }
}
