package com.vladmikhayl.e2e.dto.habit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportFullInfoResponse {

    private Long reportId;

    private boolean isCompleted;

    private LocalDateTime completionTime;

    private String photoUrl;

}
