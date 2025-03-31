package com.vladmikhayl.report.service;

import com.vladmikhayl.report.dto.ReportFullInfoResponse;
import com.vladmikhayl.report.dto.ReportStatsResponse;
import com.vladmikhayl.report.entity.FrequencyType;
import com.vladmikhayl.report.entity.Period;
import com.vladmikhayl.report.entity.Report;
import com.vladmikhayl.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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

//    public ReportStatsResponse getReportStats(
//            Long habitId,
//            FrequencyType frequencyType,
//            Set<DayOfWeek> daysOfWeek,
//            Integer timesPerWeek,
//            Integer timesPerMonth,
//            LocalDate createdAt
//    ) {
//        // Защита от случайной неверной передачи параметров разработчиком (например при тестировании)
//        validateFrequencyParams(frequencyType, daysOfWeek, timesPerWeek, timesPerMonth);
//
//        long completionsPlannedInTotal = countCompletionsPlannedInTotal(
//                frequencyType, daysOfWeek, timesPerWeek, timesPerMonth, createdAt
//        );
//
//        return null;
//    }
//
//    private void validateFrequencyParams(
//            FrequencyType frequencyType,
//            Set<DayOfWeek> daysOfWeek,
//            Integer timesPerWeek,
//            Integer timesPerMonth
//    ) {
//        if (frequencyType == FrequencyType.WEEKLY_ON_DAYS) {
//            if (daysOfWeek == null || daysOfWeek.isEmpty()) {
//                throw new IllegalArgumentException("Invalid daysOfWeek value for WEEKLY ON DAYS");
//            }
//            if (timesPerWeek != null || timesPerMonth != null) {
//                throw new IllegalArgumentException("Extra parameters are transmitted for WEEKLY ON DAYS");
//            }
//        }
//
//        if (frequencyType == FrequencyType.WEEKLY_X_TIMES) {
//            if (timesPerWeek == null) {
//                throw new IllegalArgumentException("Invalid timesPerWeek value for WEEKLY X TIMES");
//            }
//            if (daysOfWeek != null || timesPerMonth != null) {
//                throw new IllegalArgumentException("Extra parameters are transmitted for WEEKLY X TIMES");
//            }
//        }
//
//        if (frequencyType == FrequencyType.MONTHLY_X_TIMES) {
//            if (timesPerMonth == null) {
//                throw new IllegalArgumentException("Invalid timesPerMonth value for MONTHLY X TIMES");
//            }
//            if (daysOfWeek != null || timesPerWeek != null) {
//                throw new IllegalArgumentException("Extra parameters are transmitted for MONTHLY X TIMES");
//            }
//        }
//    }
//
//    private long countCompletionsPlannedInTotal(
//            FrequencyType frequencyType,
//            Set<DayOfWeek> daysOfWeek,
//            Integer timesPerWeek,
//            Integer timesPerMonth,
//            LocalDate createdAt
//    ) {
//        if (frequencyType == FrequencyType.WEEKLY_ON_DAYS) {
//            LocalDate today = LocalDate.now();
//            long totalDays = ChronoUnit.DAYS.between(createdAt, today) + 1; // Количество дней, включая оба конца
//
//            long fullWeeks = totalDays / 7; // Количество полных недель
//            long remainingDays = totalDays % 7; // Оставшиеся дни после деления на недели
//
//            long count = fullWeeks * daysOfWeek.size(); // Считаем количество вхождений дней недели в полных неделях
//
//            // Проверяем оставшиеся дни (от createdAt до today)
//            for (int i = 0; i < remainingDays; i++) {
//                LocalDate date = createdAt.plusDays(fullWeeks * 7 + i);
//                if (daysOfWeek.contains(date.getDayOfWeek())) {
//                    count++;
//                }
//            }
//
//            return count;
//        }
//
//        if (frequencyType == FrequencyType.WEEKLY_X_TIMES) {
//            return 1;
//        }
//
//        if (frequencyType == FrequencyType.MONTHLY_X_TIMES) {
//            return 1;
//        }
//
//        throw new IllegalArgumentException("There is unexpected FrequencyType value");
//    }

}
