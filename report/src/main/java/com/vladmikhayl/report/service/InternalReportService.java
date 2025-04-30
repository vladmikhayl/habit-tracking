package com.vladmikhayl.report.service;

import com.vladmikhayl.report.dto.response.HabitReportsInfoResponse;
import com.vladmikhayl.report.dto.response.ReportFullInfoResponse;
import com.vladmikhayl.report.dto.response.ReportShortInfoResponse;
import com.vladmikhayl.report.entity.FrequencyType;
import com.vladmikhayl.report.entity.Period;
import com.vladmikhayl.report.entity.Report;
import com.vladmikhayl.report.repository.ReportRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternalReportService {

    private final ReportRepository reportRepository;

    private final Clock clock;

    public ReportFullInfoResponse getReportAtDay(
            Long habitId,
            LocalDate date
    ) {
        Optional<Report> report = reportRepository.findByHabitIdAndDate(habitId, date);

        if (report.isEmpty()) {
            return ReportFullInfoResponse.builder()
                    .reportId(null)
                    .isCompleted(false)
                    .completionTime(null)
                    .photoUrl(null)
                    .build();
        }

        return ReportFullInfoResponse.builder()
                .reportId(report.get().getId())
                .isCompleted(true)
                .completionTime(report.get().getCreatedAt())
                .photoUrl(report.get().getPhotoUrl())
                .build();
    }

    public ReportShortInfoResponse isCompletedAtDay(
            Long habitId,
            LocalDate date
    ) {
        boolean isCompleted = reportRepository.existsByHabitIdAndDate(habitId, date);
        boolean isPhotoUploaded = false;
        Long reportId = null;

        if (isCompleted) {
            Report report = reportRepository.findByHabitIdAndDate(habitId, date)
                    .orElseThrow(() -> new EntityNotFoundException("Report not found"));
            isPhotoUploaded = report.getPhotoUrl() != null;
            reportId = report.getId();
        }

        return ReportShortInfoResponse.builder()
                .reportId(reportId)
                .isCompleted(isCompleted)
                .isPhotoUploaded(isPhotoUploaded)
                .build();
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

    public HabitReportsInfoResponse getReportsInfo(
            Long habitId,
            FrequencyType frequencyType,
            Set<DayOfWeek> daysOfWeek,
            Integer timesPerWeek,
            Integer timesPerMonth,
            LocalDate createdAt
    ) {
        // Защита от случайной неверной передачи параметров разработчиком (например при тестировании)
        validateFrequencyParams(frequencyType, daysOfWeek, timesPerWeek, timesPerMonth);

        int completionsInTotal = reportRepository.countByHabitId(habitId);

        Integer completionsPercent = null;

        if (frequencyType == FrequencyType.WEEKLY_ON_DAYS) {
            long completionsPlannedInTotal = countCompletionsPlannedInTotal(daysOfWeek, createdAt);
            if (completionsPlannedInTotal != 0) {
                completionsPercent = (int) (completionsInTotal / (double)completionsPlannedInTotal * 100);
            }
        }

        Integer completionsInPeriod = null;

        if (frequencyType == FrequencyType.WEEKLY_X_TIMES) {
            completionsInPeriod = countCompletionsInPeriod(habitId, Period.WEEK, LocalDate.now(clock));
        }

        if (frequencyType == FrequencyType.MONTHLY_X_TIMES) {
            completionsInPeriod = countCompletionsInPeriod(habitId, Period.MONTH, LocalDate.now(clock));
        }

        Integer completionsPlannedInPeriod = null;

        if (frequencyType == FrequencyType.WEEKLY_X_TIMES) {
            completionsPlannedInPeriod = timesPerWeek;
        }

        if (frequencyType == FrequencyType.MONTHLY_X_TIMES) {
            completionsPlannedInPeriod = timesPerMonth;
        }

        List<LocalDate> completedDays = getCompletedDays(habitId);

        List<LocalDate> uncompletedDays = null;

        if (frequencyType == FrequencyType.WEEKLY_ON_DAYS) {
            uncompletedDays = getUncompletedDays(habitId, daysOfWeek, createdAt);
        }

        Integer serialDays = null;

        if (frequencyType == FrequencyType.WEEKLY_ON_DAYS) {
            boolean shouldSerialDaysBeNull = (uncompletedDays.isEmpty() || uncompletedDays.equals(List.of(LocalDate.now(clock))))
                    && completionsInTotal == 0;
            if (!shouldSerialDaysBeNull) {
                serialDays = countSerialDays(habitId, daysOfWeek);
            }
        }

        return HabitReportsInfoResponse.builder()
                .completionsInTotal(completionsInTotal)
                .completionsPercent(completionsPercent)
                .serialDays(serialDays)
                .completionsInPeriod(completionsInPeriod)
                .completionsPlannedInPeriod(completionsPlannedInPeriod)
                .completedDays(completedDays)
                .uncompletedDays(uncompletedDays)
                .build();
    }

    // Вызывается для всех привычек
    private void validateFrequencyParams(
            FrequencyType frequencyType,
            Set<DayOfWeek> daysOfWeek,
            Integer timesPerWeek,
            Integer timesPerMonth
    ) {
        if (frequencyType == FrequencyType.WEEKLY_ON_DAYS) {
            if (daysOfWeek == null || daysOfWeek.isEmpty()) {
                throw new IllegalArgumentException("Invalid daysOfWeek value for WEEKLY ON DAYS");
            }
            if (timesPerWeek != null || timesPerMonth != null) {
                throw new IllegalArgumentException("Extra parameters are transmitted for WEEKLY ON DAYS");
            }
        }

        if (frequencyType == FrequencyType.WEEKLY_X_TIMES) {
            if (timesPerWeek == null) {
                throw new IllegalArgumentException("Invalid timesPerWeek value for WEEKLY X TIMES");
            }
            if (daysOfWeek != null || timesPerMonth != null) {
                throw new IllegalArgumentException("Extra parameters are transmitted for WEEKLY X TIMES");
            }
        }

        if (frequencyType == FrequencyType.MONTHLY_X_TIMES) {
            if (timesPerMonth == null) {
                throw new IllegalArgumentException("Invalid timesPerMonth value for MONTHLY X TIMES");
            }
            if (daysOfWeek != null || timesPerWeek != null) {
                throw new IllegalArgumentException("Extra parameters are transmitted for MONTHLY X TIMES");
            }
        }
    }

    // Вызывается только для привычек WEEKLY ON DAYS
    private long countCompletionsPlannedInTotal(Set<DayOfWeek> daysOfWeek, LocalDate createdAt) {
        LocalDate today = LocalDate.now(clock);
        long totalDays = ChronoUnit.DAYS.between(createdAt, today) + 1; // Количество дней, включая оба конца

        long fullWeeks = totalDays / 7; // Количество полных недель
        long remainingDays = totalDays % 7; // Оставшиеся дни после деления на недели

        long count = fullWeeks * daysOfWeek.size(); // Считаем количество вхождений дней недели в полных неделях

        // Проверяем оставшиеся дни (от createdAt до today)
        for (int i = 0; i < remainingDays; i++) {
            LocalDate date = createdAt.plusDays(fullWeeks * 7 + i);
            if (daysOfWeek.contains(date.getDayOfWeek())) {
                count++;
            }
        }

        return count;
    }

    // Вызывается только для привычек WEEKLY ON DAYS
    private int countSerialDays(Long habitId, Set<DayOfWeek> dayOfWeeks) {
        int count = 0;

        for (LocalDate date = LocalDate.now(clock).minusDays(1); true; date = date.minusDays(1)) {
            if (!dayOfWeeks.contains(date.getDayOfWeek())) {
                continue;
            }
            boolean isCompleted = reportRepository.existsByHabitIdAndDate(habitId, date);
            if (isCompleted) {
                count++;
            } else {
                break;
            }
        }

        boolean isCompletedToday = reportRepository.existsByHabitIdAndDate(habitId, LocalDate.now(clock));
        if (isCompletedToday) {
            count++;
        }

        return count;
    }

    // Вызывается для всех привычек
    private List<LocalDate> getCompletedDays(Long habitId) {
        return reportRepository.findAllByHabitId(habitId).stream()
                .map(Report::getDate)
                .collect(Collectors.toList());
    }

    // Вызывается только для привычек WEEKLY ON DAYS
    private List<LocalDate> getUncompletedDays(Long habitId, Set<DayOfWeek> daysOfWeek, LocalDate createdAt) {
        List<LocalDate> completedDays = getCompletedDays(habitId);

        List<LocalDate> allDaysFromHabitCreating = createdAt.datesUntil(LocalDate.now(clock).plusDays(1))
                .toList();

        return allDaysFromHabitCreating.stream()
                .filter(date -> (!completedDays.contains(date) && daysOfWeek.contains(date.getDayOfWeek())))
                .collect(Collectors.toList());
    }

}
