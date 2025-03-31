package com.vladmikhayl.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@Schema(description = "Запрос на создание отчета о выполнении привычки")
public class ReportCreationRequest {

    @NotNull(message = "Habit ID must be specified")
    @Schema(description = "ID привычки", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long habitId;

    @NotNull(message = "Date must be specified")
    @Schema(description = "Дата, в которую привычка выполнена", example = "2025-03-30", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate date;

    @Schema(description = "URL прикрепляемого фото", example = "https://i.pinimg.com/736x/b9/a7/55/b9a75516248779bead50d84c52daebf3.jpg")
    private String photoUrl;

}
