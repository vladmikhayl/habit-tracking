package com.vladmikhayl.report.controller;

import com.vladmikhayl.report.dto.ReportCreationRequest;
import com.vladmikhayl.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/create")
    public ResponseEntity<Void> createReport(
            @Valid @RequestBody ReportCreationRequest reportCreationRequest,
            @RequestHeader("X-User-Id") String userId
    ) {
        reportService.createReport(reportCreationRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
