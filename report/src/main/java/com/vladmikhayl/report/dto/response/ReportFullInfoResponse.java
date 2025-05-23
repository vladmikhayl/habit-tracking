package com.vladmikhayl.report.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportFullInfoResponse {

    private Long reportId;

    private boolean isCompleted;

    private LocalDateTime completionTime;

    private String photoUrl;

}
