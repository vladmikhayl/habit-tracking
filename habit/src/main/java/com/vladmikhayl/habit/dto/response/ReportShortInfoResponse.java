package com.vladmikhayl.habit.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportShortInfoResponse {

    private boolean isCompleted;

    private boolean isPhotoUploaded;

}
