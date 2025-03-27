package com.vladmikhayl.report.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ReportCreationRequest {

    @NotNull(message = "Habit ID must be specified")
    private Long habitId;

    @NotNull(message = "Date must be specified")
    private LocalDate date;

    private String photoUrl;

}
