package com.vladmikhayl.report.controller;

import com.vladmikhayl.report.dto.ReportCreationRequest;
import com.vladmikhayl.report.dto.ReportPhotoEditingRequest;
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
            @Valid @RequestBody ReportCreationRequest request,
            @RequestHeader("X-User-Id") String userId
    ) {
        reportService.createReport(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{reportId}/change-photo")
    public ResponseEntity<Void> changeReportPhoto(
            @PathVariable Long reportId,
            @RequestBody ReportPhotoEditingRequest request,
            @RequestHeader("X-User-Id") String userId
    ) {
        reportService.changeReportPhoto(reportId, request, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{reportId}/delete")
    public ResponseEntity<Void> deleteReport(
            @PathVariable Long reportId,
            @RequestHeader("X-User-Id") String userId
    ) {
        reportService.deleteReport(reportId, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
