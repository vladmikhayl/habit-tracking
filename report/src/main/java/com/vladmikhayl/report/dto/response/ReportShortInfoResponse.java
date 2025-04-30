package com.vladmikhayl.report.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportShortInfoResponse {

    private boolean isCompleted;

    private boolean isPhotoUploaded;

}
