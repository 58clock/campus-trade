package com.campus.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportVO {

    private Long id;
    private Long reporterId;
    private String reporterName;
    private String targetType;
    private Long targetId;
    private String reason;
    private String status;
    private String handlerNote;
    private LocalDateTime createdAt;
}
