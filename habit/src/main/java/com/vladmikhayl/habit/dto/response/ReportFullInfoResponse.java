package com.vladmikhayl.habit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Информация об отчете о выполнении привычки за конкретный день")
public class ReportFullInfoResponse {

    @Schema(description = "ID отчета", example = "25")
    private Long reportId;

    @Schema(description = "Выполнена ли привычка в этот день", example = "true")
    private boolean isCompleted;

    @Schema(description = "Дата и время выполнения (только если выполнена, иначе null)")
    private LocalDateTime completionTime;

    @Schema(description = "URL прикрепленного фото (только если есть, иначе null)", example = "https://i.pinimg.com/736x/b9/a7/55/b9a75516248779bead50d84c52daebf3.jpg")
    private String photoUrl;

}
