package com.vladmikhayl.report.service;

import com.vladmikhayl.report.dto.ReportFullInfoResponse;
import com.vladmikhayl.report.entity.Period;
import com.vladmikhayl.report.entity.Report;
import com.vladmikhayl.report.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private InternalReportService underTest;

    @Test
    void canGetReportAtDayWithPhotoWhenReportIsPresent() {
        Long reportId = 2L;
        Long habitId = 5L;
        LocalDate date = LocalDate.of(2025, 3, 29);
        LocalDateTime createdAt = LocalDateTime.of(2025, 3, 28, 12, 50, 30);

        Report report = Report.builder()
                .id(reportId)
                .habitId(habitId)
                .date(date)
                .photoUrl("https://photo-url.com/")
                .createdAt(createdAt)
                .build();

        when(reportRepository.findByHabitIdAndDate(habitId, date)).thenReturn(Optional.of(report));

        ReportFullInfoResponse response = underTest.getReportAtDay(habitId, date);

        assertThat(response.isCompleted()).isTrue();
        assertThat(response.getCompletionTime()).isEqualTo(createdAt);
        assertThat(response.getPhotoUrl()).isEqualTo("https://photo-url.com/");
    }

    @Test
    void canGetReportAtDayWithoutPhotoWhenReportIsPresent() {
        Long reportId = 2L;
        Long habitId = 5L;
        LocalDate date = LocalDate.of(2025, 3, 29);
        LocalDateTime createdAt = LocalDateTime.of(2025, 3, 28, 12, 50, 30);

        Report report = Report.builder()
                .id(reportId)
                .habitId(habitId)
                .date(date)
                .photoUrl(null)
                .createdAt(createdAt)
                .build();

        when(reportRepository.findByHabitIdAndDate(habitId, date)).thenReturn(Optional.of(report));

        ReportFullInfoResponse response = underTest.getReportAtDay(habitId, date);

        assertThat(response.isCompleted()).isTrue();
        assertThat(response.getCompletionTime()).isEqualTo(createdAt);
        assertThat(response.getPhotoUrl()).isNull();
    }

    @Test
    void canGetReportAtDayWhenReportIsNotPresent() {
        Long habitId = 5L;
        LocalDate date = LocalDate.of(2025, 3, 29);

        when(reportRepository.findByHabitIdAndDate(habitId, date)).thenReturn(Optional.empty());

        ReportFullInfoResponse response = underTest.getReportAtDay(habitId, date);

        assertThat(response.isCompleted()).isFalse();
        assertThat(response.getCompletionTime()).isNull();
        assertThat(response.getPhotoUrl()).isNull();
    }

    @Test
    void canCountCompletionsInWeekPeriod() {
        Long habitId = 5L;
        LocalDate date = LocalDate.of(2025, 3, 29);
        LocalDate startDate = LocalDate.of(2025, 3, 24);
        LocalDate endDate = LocalDate.of(2025, 3, 30);

        when(reportRepository.countByHabitIdAndDateBetween(habitId, startDate, endDate)).thenReturn(3);

        int count = underTest.countCompletionsInPeriod(habitId, Period.WEEK, date);

        assertThat(count).isEqualTo(3);
    }

    @Test
    void canCountCompletionsInWeekPeriodWithDifferentYears() {
        Long habitId = 5L;
        LocalDate date = LocalDate.of(2024, 12, 30);
        LocalDate startDate = LocalDate.of(2024, 12, 30);
        LocalDate endDate = LocalDate.of(2025, 1, 5);

        when(reportRepository.countByHabitIdAndDateBetween(habitId, startDate, endDate)).thenReturn(3);

        int count = underTest.countCompletionsInPeriod(habitId, Period.WEEK, date);

        assertThat(count).isEqualTo(3);
    }

    @Test
    void canCountCompletionsInMonthPeriod() {
        Long habitId = 5L;
        LocalDate date = LocalDate.of(2025, 3, 29);
        LocalDate startDate = LocalDate.of(2025, 3, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);

        when(reportRepository.countByHabitIdAndDateBetween(habitId, startDate, endDate)).thenReturn(10);

        int count = underTest.countCompletionsInPeriod(habitId, Period.MONTH, date);

        assertThat(count).isEqualTo(10);
    }

    @Test
    void canCountCompletionsInShortMonthPeriod() {
        Long habitId = 5L;
        LocalDate date = LocalDate.of(2025, 2, 28);
        LocalDate startDate = LocalDate.of(2025, 2, 1);
        LocalDate endDate = LocalDate.of(2025, 2, 28);

        when(reportRepository.countByHabitIdAndDateBetween(habitId, startDate, endDate)).thenReturn(10);

        int count = underTest.countCompletionsInPeriod(habitId, Period.MONTH, date);

        assertThat(count).isEqualTo(10);
    }

}