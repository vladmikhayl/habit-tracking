package com.vladmikhayl.report.controller;

import com.vladmikhayl.report.dto.ReportFullInfoResponse;
import com.vladmikhayl.report.service.InternalReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/internal/reports")
@RequiredArgsConstructor
public class InternalReportController {

    private final InternalReportService internalReportService;

    @GetMapping("/get-report/of-habit/{habitId}/at-day/{date}")
    public ResponseEntity<ReportFullInfoResponse> getReportAtDay(
            @PathVariable Long habitId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        ReportFullInfoResponse reportFullInfo = internalReportService.getReportAtDay(habitId, date);
        return ResponseEntity.ok(reportFullInfo);
    }

}
