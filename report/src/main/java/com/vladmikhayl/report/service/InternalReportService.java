package com.vladmikhayl.report.service;

import com.vladmikhayl.report.dto.ReportFullInfoResponse;
import com.vladmikhayl.report.entity.Period;
import com.vladmikhayl.report.entity.Report;
import com.vladmikhayl.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InternalReportService {

    private final ReportRepository reportRepository;

    public ReportFullInfoResponse getReportAtDay(
            Long habitId,
            LocalDate date
    ) {
        Optional<Report> report = reportRepository.findByHabitIdAndDate(habitId, date);

        if (report.isEmpty()) {
            return ReportFullInfoResponse.builder()
                    .isCompleted(false)
                    .completionTime(null)
                    .photoUrl(null)
                    .build();
        }

        return ReportFullInfoResponse.builder()
                .isCompleted(true)
                .completionTime(report.get().getCreatedAt())
                .photoUrl(report.get().getPhotoUrl())
                .build();
    }

    public boolean isCompletedAtDay(
            Long habitId,
            LocalDate date
    ) {
        return reportRepository.existsByHabitIdAndDate(habitId, date);
    }

    public int countCompletionsInPeriod(
            Long habitId,
            Period period,
            LocalDate date
    ) {
        LocalDate startDate;
        LocalDate endDate;

        if (period == Period.WEEK) {
            startDate = date.with(DayOfWeek.MONDAY);
            endDate = startDate.plusDays(6);
        } else {
            startDate = date.withDayOfMonth(1);
            endDate = date.withDayOfMonth(date.lengthOfMonth());
        }

        return reportRepository.countByHabitIdAndDateBetween(habitId, startDate, endDate);
    }

}
