package com.vladmikhayl.report.service;

import com.vladmikhayl.report.dto.request.ReportCreationRequest;
import com.vladmikhayl.report.dto.request.ReportPhotoEditingRequest;
import com.vladmikhayl.report.entity.Report;
import com.vladmikhayl.report.repository.HabitPhotoAllowedCacheRepository;
import com.vladmikhayl.report.repository.ReportRepository;
import com.vladmikhayl.report.service.feign.HabitClient;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    private final HabitPhotoAllowedCacheRepository habitPhotoAllowedCacheRepository;

    private final HabitClient habitClient;

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid user ID format");
        }
    }

    // TODO: проверять что URL фото правильное
    public void createReport(
            ReportCreationRequest request,
            String userId
    ) {
        Long userIdLong = parseUserId(userId);

        boolean isHabitCurrentAtThatDateForThatUser = habitClient.isCurrent(
                request.getHabitId(),
                userIdLong,
                request.getDate()
        ).getBody();

        if (!isHabitCurrentAtThatDateForThatUser) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This user doesn't have this habit on this day");
        }

        Long habitId = request.getHabitId();

        if (reportRepository.existsByHabitIdAndDate(habitId, request.getDate())) {
            throw new DataIntegrityViolationException("This habit has already been marked as completed on this day");
        }

        if (request.getPhotoUrl() != null && !habitPhotoAllowedCacheRepository.existsById(habitId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This habit doesn't imply a photo, but it was attached");
        }

        if (request.getDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "It is forbidden to mark a habit as completed for a day that has not yet arrived");
        }

        Report report = Report.builder()
                .userId(userIdLong)
                .habitId(habitId)
                .date(request.getDate())
                .photoUrl(request.getPhotoUrl())
                .build();

        reportRepository.save(report);
    }

    // TODO: проверять что URL фото правильное
    @Transactional
    public void changeReportPhoto(
            Long reportId,
            ReportPhotoEditingRequest request,
            String userId
    ) {
        Long userIdLong = parseUserId(userId);

        Report report = reportRepository.findByIdAndUserId(reportId, userIdLong)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "This user doesn't have this report"));

        long habitId = report.getHabitId();

        if (!habitPhotoAllowedCacheRepository.existsById(habitId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This habit doesn't imply a photo");
        }

        String photoUrl = request.getPhotoUrl();

        if (photoUrl == null) {
            return;
        }

        // Чтобы положить значение null в поле photoUrl, в качестве этого параметра нужно передать пустую строку
        if (photoUrl.isEmpty()) {
            report.setPhotoUrl(null);
        } else {
            report.setPhotoUrl(photoUrl);
        }
    }

    public void deleteReport(
            Long reportId,
            String userId
    ) {
        Long userIdLong = parseUserId(userId);

        if (!reportRepository.existsByIdAndUserId(reportId, userIdLong)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This user doesn't have this report");
        }

        reportRepository.deleteById(reportId);
    }

}
