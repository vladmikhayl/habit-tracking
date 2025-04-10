package com.vladmikhayl.report.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ReportStatsResponse {

    // Для всех привычек
    private int completionsInTotal;

    // Только для привычек с WEEKLY ON DAYS (иначе null)
    // Если еще не начался ни один день, в который нужно выполнить привычку, то сюда кладется null
    private Integer completionsPercent;

    // Только для привычек с WEEKLY ON DAYS (иначе null)
    // Если еще не завершился ни один день, в который нужно выполнить привычку, и при этом привычка не была выполнена
    // в текущий день, то сюда кладется null
    private Integer serialDays;

    // Только для привычек с WEEKLY X TIMES или MONTHLY X TIMES (иначе null)
    private Integer completionsInPeriod;

    // Только для привычек с WEEKLY X TIMES или MONTHLY X TIMES (иначе null)
    private Integer completionsPlannedInPeriod;

    // Для всех привычек
    private List<LocalDate> completedDays;

    // Только для привычек с WEEKLY ON DAYS (иначе null)
    private List<LocalDate> uncompletedDays;

}
