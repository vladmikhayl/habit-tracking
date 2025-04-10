package com.vladmikhayl.report.controller;

import com.vladmikhayl.report.dto.response.ReportFullInfoResponse;
import com.vladmikhayl.report.dto.response.ReportStatsResponse;
import com.vladmikhayl.report.entity.FrequencyType;
import com.vladmikhayl.report.entity.Period;
import com.vladmikhayl.report.service.InternalReportService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/internal/reports")
@RequiredArgsConstructor
@Hidden
public class InternalReportController {

    private final InternalReportService internalReportService;

    @GetMapping("/get-report/of-habit/{habitId}/at-day/{date}")
    public ResponseEntity<ReportFullInfoResponse> getReportAtDay(
            @PathVariable Long habitId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        ReportFullInfoResponse response = internalReportService.getReportAtDay(habitId, date);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{habitId}/is-completed/at-day/{date}")
    public ResponseEntity<Boolean> isCompletedAtDay(
            @PathVariable Long habitId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        boolean isCompleted = internalReportService.isCompletedAtDay(habitId, date);
        return ResponseEntity.ok(isCompleted);
    }

    @GetMapping("/{habitId}/completion-count/{period}/at/{date}")
    public ResponseEntity<Integer> countCompletionsInPeriod(
            @PathVariable Long habitId,
            @PathVariable Period period,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        int count = internalReportService.countCompletionsInPeriod(habitId, period, date);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{habitId}/reports-info")
    public ResponseEntity<ReportStatsResponse> getReportStats(
            @PathVariable Long habitId,
            @RequestParam FrequencyType frequencyType,
            @RequestParam(required = false) Set<DayOfWeek> daysOfWeek,
            @RequestParam(required = false) Integer timesPerWeek,
            @RequestParam(required = false) Integer timesPerMonth,
            @RequestParam LocalDate createdAt
    ) {
        ReportStatsResponse response = internalReportService.getReportStats(
                habitId, frequencyType, daysOfWeek, timesPerWeek, timesPerMonth, createdAt
        );
        return ResponseEntity.ok(response);
    }

}
