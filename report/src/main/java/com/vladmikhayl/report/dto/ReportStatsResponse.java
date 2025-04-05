package com.vladmikhayl.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ReportStatsResponse {

    // Для всех привычек
    private int completionsInTotal;

    // Только для привычек с WEEKLY ON DAYS
    private Integer completionsPercent;

    // Только для привычек с WEEKLY ON DAYS
    private Integer serialDays;

    // Только для привычек с WEEKLY X TIMES или MONTHLY X TIMES
    private Integer completionsInPeriod;

    // Только для привычек с WEEKLY X TIMES или MONTHLY X TIMES
    private Integer completionsPlannedInPeriod;

    // Для всех привычек
    private List<LocalDate> completedDays;

    // Только для привычек с WEEKLY ON DAYS
    private List<LocalDate> uncompletedDays;

}
