package com.vladmikhayl.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportFullInfoResponse {

    private boolean isCompleted;

    private LocalDateTime completionTime;

    private String photoUrl;

}
