package com.vladmikhayl.report.service;

import com.vladmikhayl.report.dto.ReportCreationRequest;
import com.vladmikhayl.report.entity.Report;
import com.vladmikhayl.report.repository.HabitPhotoAllowedCacheRepository;
import com.vladmikhayl.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    private final HabitPhotoAllowedCacheRepository habitPhotoAllowedCacheRepository;

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid user ID format");
        }
    }

    public void createReport(
            ReportCreationRequest request,
            String userId
    ) {

        Long userIdLong = parseUserId(userId);

        boolean isHabitCurrentAtThatDateForThatUser = true;

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

}
