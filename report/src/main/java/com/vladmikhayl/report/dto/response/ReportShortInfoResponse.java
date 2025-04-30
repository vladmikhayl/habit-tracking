package com.vladmikhayl.report.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportShortInfoResponse {

    private Long reportId;

    private boolean isCompleted;

    private boolean isPhotoUploaded;

}
