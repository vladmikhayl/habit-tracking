package com.vladmikhayl.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ReportStatsResponse {

    private int completionsPercent;

    private Integer serialDays;

    private Integer completionsInPeriod;

    private Integer completionsPlannedInPeriod;

    private List<LocalDate> completedDays;

    private List<LocalDate> uncompletedDays;

}
