package com.campus.service;

import com.campus.common.Result;
import com.campus.entity.Report;
import com.campus.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;

    public Result<Void> submitReport(Long reporterId, String targetType, Long targetId, String reason) {
        // TODO: D - 创建举报记录
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setStatus("PENDING");
        reportMapper.insert(report);
        return Result.ok();
    }
}
