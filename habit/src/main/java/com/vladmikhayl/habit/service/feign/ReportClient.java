package com.vladmikhayl.habit.service.feign;

import com.vladmikhayl.habit.dto.response.ReportFullInfoResponse;
import com.vladmikhayl.habit.dto.response.HabitReportsInfoResponse;
import com.vladmikhayl.habit.entity.FrequencyType;
import com.vladmikhayl.habit.entity.Period;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@FeignClient(name = "report")
@Profile("!test") // чтобы в интеграционных тестах бин этого Feign-клиента не поднимался (там будет свой бин с замоканным ответом)
public interface ReportClient {

    @GetMapping("/internal/reports/{habitId}/reports-info")
    ResponseEntity<HabitReportsInfoResponse> getReportsInfo(
            @PathVariable Long habitId,
            @RequestParam FrequencyType frequencyType,
            @RequestParam(required = false) Set<DayOfWeek> daysOfWeek,
            @RequestParam(required = false) Integer timesPerWeek,
            @RequestParam(required = false) Integer timesPerMonth,
            @RequestParam LocalDate createdAt
    );

    @GetMapping("/internal/reports/get-report/of-habit/{habitId}/at-day/{date}")
    ResponseEntity<ReportFullInfoResponse> getReportAtDay(
            @PathVariable Long habitId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );

    @GetMapping("/internal/reports/{habitId}/is-completed/at-day/{date}")
    ResponseEntity<Boolean> isCompletedAtDay(
            @PathVariable Long habitId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );

    @GetMapping("/internal/reports/{habitId}/completion-count/{period}/at/{date}")
    ResponseEntity<Integer> countCompletionsInPeriod(
            @PathVariable Long habitId,
            @PathVariable Period period,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );

}
