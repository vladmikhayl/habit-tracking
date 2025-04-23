package com.vladmikhayl.e2e.dto.habit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HabitReportsInfoResponse {

    private int completionsInTotal;

    private Integer completionsPercent;

    private Integer serialDays;

    private Integer completionsInPeriod;

    private Integer completionsPlannedInPeriod;

    private List<LocalDate> completedDays;

    private List<LocalDate> uncompletedDays;

}
