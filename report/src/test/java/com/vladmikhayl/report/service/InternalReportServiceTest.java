package com.vladmikhayl.report.service;

import com.vladmikhayl.report.dto.ReportFullInfoResponse;
import com.vladmikhayl.report.dto.ReportStatsResponse;
import com.vladmikhayl.report.entity.FrequencyType;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test // WEEKLY ON DAYS во все дни, создана сегодня, не выполнена ни разу
    void testReportStatsForWeeklyOnDaysEveryDayThatCreatedTodayWithZeroCompletions() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.WEEKLY_ON_DAYS;
        Set<DayOfWeek> daysOfWeek = Set.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
        );
        LocalDate createdAt = LocalDate.now();

        when(reportRepository.countByHabitId(habitId)).thenReturn(0);

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(List.of());

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                daysOfWeek,
                null,
                null,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(0);
        assertThat(response.getCompletionsPercent()).isEqualTo(0);
        assertThat(response.getSerialDays()).isNull();
        assertThat(response.getCompletionsInPeriod()).isNull();
        assertThat(response.getCompletionsPlannedInPeriod()).isNull();
        assertThat(response.getCompletedDays()).isEqualTo(List.of());
        assertThat(response.getUncompletedDays()).isEqualTo(List.of(LocalDate.now()));
    }

    @Test // WEEKLY ON DAYS только во вчерашний день, создана сегодня, не выполнена ни разу
    void testReportStatsForWeeklyOnDaysOnYesterdayThatCreatedTodayWithZeroCompletions() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.WEEKLY_ON_DAYS;
        Set<DayOfWeek> daysOfWeek = Set.of(
                LocalDate.now().minusDays(1).getDayOfWeek()
        );
        LocalDate createdAt = LocalDate.now();

        when(reportRepository.countByHabitId(habitId)).thenReturn(0);

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(List.of());

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                daysOfWeek,
                null,
                null,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(0);
        assertThat(response.getCompletionsPercent()).isNull();
        assertThat(response.getSerialDays()).isNull();
        assertThat(response.getCompletionsInPeriod()).isNull();
        assertThat(response.getCompletionsPlannedInPeriod()).isNull();
        assertThat(response.getCompletedDays()).isEqualTo(List.of());
        assertThat(response.getUncompletedDays()).isEqualTo(List.of());
    }

    @Test // WEEKLY ON DAYS в сегодняшний и вчерашний день, создана сегодня, выполнена сегодня
    void testReportStatsForWeeklyOnDaysOnTodayAndYesterdayThatCreatedTodayWithOneCompletion() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.WEEKLY_ON_DAYS;
        Set<DayOfWeek> daysOfWeek = Set.of(
                LocalDate.now().getDayOfWeek(),
                LocalDate.now().minusDays(1).getDayOfWeek()
        );
        LocalDate createdAt = LocalDate.now();

        when(reportRepository.countByHabitId(habitId)).thenReturn(1);

        when(reportRepository.existsByHabitIdAndDate(eq(habitId), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate date = invocation.getArgument(1);
                    return date.equals(LocalDate.now());
                });

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(List.of(
                Report.builder()
                        .date(LocalDate.now())
                        .build()
        ));

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                daysOfWeek,
                null,
                null,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(1);
        assertThat(response.getCompletionsPercent()).isEqualTo(100);
        assertThat(response.getSerialDays()).isEqualTo(1);
        assertThat(response.getCompletionsInPeriod()).isNull();
        assertThat(response.getCompletionsPlannedInPeriod()).isNull();
        assertThat(response.getCompletedDays()).isEqualTo(List.of(
                LocalDate.now()
        ));
        assertThat(response.getUncompletedDays()).isEqualTo(List.of());
    }

    @Test // WEEKLY ON DAYS в сегодняшний и вчерашний день, создана вчера, выполнена сегодня
    void testReportStatsForWeeklyOnDaysOnTodayAndYesterdayThatCreatedYesterdayWithOneCompletion() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.WEEKLY_ON_DAYS;
        Set<DayOfWeek> daysOfWeek = Set.of(
                LocalDate.now().getDayOfWeek(),
                LocalDate.now().minusDays(1).getDayOfWeek()
        );
        LocalDate createdAt = LocalDate.now().minusDays(1);

        when(reportRepository.countByHabitId(habitId)).thenReturn(1);

        when(reportRepository.existsByHabitIdAndDate(eq(habitId), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate date = invocation.getArgument(1);
                    return date.equals(LocalDate.now());
                });

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(List.of(
                Report.builder()
                        .date(LocalDate.now())
                        .build()
        ));

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                daysOfWeek,
                null,
                null,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(1);
        assertThat(response.getCompletionsPercent()).isEqualTo(50);
        assertThat(response.getSerialDays()).isEqualTo(1);
        assertThat(response.getCompletionsInPeriod()).isNull();
        assertThat(response.getCompletionsPlannedInPeriod()).isNull();
        assertThat(response.getCompletedDays()).isEqualTo(List.of(
                LocalDate.now()
        ));
        assertThat(response.getUncompletedDays()).isEqualTo(List.of(
                LocalDate.now().minusDays(1)
        ));
    }

    @Test // WEEKLY ON DAYS в сегодняшний и вчерашний день, создана позавчера, выполнена сегодня и вчера
    void testReportStatsForWeeklyOnDaysOnTodayAndYesterdayThatCreatedTwoDaysAgoWithTwoCompletions() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.WEEKLY_ON_DAYS;
        Set<DayOfWeek> daysOfWeek = Set.of(
                LocalDate.now().getDayOfWeek(),
                LocalDate.now().minusDays(1).getDayOfWeek()
        );
        LocalDate createdAt = LocalDate.now().minusDays(2);

        when(reportRepository.countByHabitId(habitId)).thenReturn(2);

        when(reportRepository.existsByHabitIdAndDate(eq(habitId), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate date = invocation.getArgument(1);
                    return date.equals(LocalDate.now()) || date.equals(LocalDate.now().minusDays(1));
                });

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(List.of(
                Report.builder()
                        .date(LocalDate.now())
                        .build(),
                Report.builder()
                        .date(LocalDate.now().minusDays(1))
                        .build()
        ));

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                daysOfWeek,
                null,
                null,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(2);
        assertThat(response.getCompletionsPercent()).isEqualTo(100);
        assertThat(response.getSerialDays()).isEqualTo(2);
        assertThat(response.getCompletionsInPeriod()).isNull();
        assertThat(response.getCompletionsPlannedInPeriod()).isNull();
        assertThat(response.getCompletedDays()).isEqualTo(List.of(
                LocalDate.now(),
                LocalDate.now().minusDays(1)
        ));
        assertThat(response.getUncompletedDays()).isEqualTo(List.of());
    }

    @Test // WEEKLY ON DAYS только в сегодняшний день, создана месяц назад, не выполнена ни разу
    void testReportStatsForWeeklyOnDaysOnTodayThatCreatedMonthAgoWithZeroCompletions() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.WEEKLY_ON_DAYS;
        Set<DayOfWeek> daysOfWeek = Set.of(
                LocalDate.now().getDayOfWeek()
        );
        LocalDate createdAt = LocalDate.now().minusDays(30);

        when(reportRepository.countByHabitId(habitId)).thenReturn(0);

        when(reportRepository.existsByHabitIdAndDate(eq(habitId), any(LocalDate.class))).thenReturn(false);

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(List.of());

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                daysOfWeek,
                null,
                null,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(0);
        assertThat(response.getCompletionsPercent()).isEqualTo(0);
        assertThat(response.getSerialDays()).isEqualTo(0);
        assertThat(response.getCompletionsInPeriod()).isNull();
        assertThat(response.getCompletionsPlannedInPeriod()).isNull();
        assertThat(response.getCompletedDays()).isEqualTo(List.of());
        assertThat(response.getUncompletedDays()).containsExactlyInAnyOrder(
                LocalDate.now(),
                LocalDate.now().minusDays(7),
                LocalDate.now().minusDays(14),
                LocalDate.now().minusDays(21),
                LocalDate.now().minusDays(28)
        );
    }

    @Test
        // WEEKLY ON DAYS только в сегодняшний день, создана месяц назад, выполнена 3 раза
        // (из них 2 в эту серию, причем сегодня еще не выполнена)
    void testReportStatsForWeeklyOnDaysOnTodayThatCreatedMonthAgoWithThreeCompletions() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.WEEKLY_ON_DAYS;
        Set<DayOfWeek> daysOfWeek = Set.of(
                LocalDate.now().getDayOfWeek()
        );
        LocalDate createdAt = LocalDate.now().minusDays(30);

        when(reportRepository.countByHabitId(habitId)).thenReturn(3);

        when(reportRepository.existsByHabitIdAndDate(eq(habitId), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate date = invocation.getArgument(1);
                    return date.equals(LocalDate.now().minusDays(7)) ||
                            date.equals(LocalDate.now().minusDays(14)) ||
                            date.equals(LocalDate.now().minusDays(28));
                });

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(List.of(
                Report.builder()
                        .date(LocalDate.now().minusDays(7))
                        .build(),
                Report.builder()
                        .date(LocalDate.now().minusDays(14))
                        .build(),
                Report.builder()
                        .date(LocalDate.now().minusDays(28))
                        .build()
        ));

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                daysOfWeek,
                null,
                null,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(3);
        assertThat(response.getCompletionsPercent()).isEqualTo(60);
        assertThat(response.getSerialDays()).isEqualTo(2);
        assertThat(response.getCompletionsInPeriod()).isNull();
        assertThat(response.getCompletionsPlannedInPeriod()).isNull();
        assertThat(response.getCompletedDays()).containsExactlyInAnyOrder(
                LocalDate.now().minusDays(7),
                LocalDate.now().minusDays(14),
                LocalDate.now().minusDays(28)
        );
        assertThat(response.getUncompletedDays()).containsExactlyInAnyOrder(
                LocalDate.now(),
                LocalDate.now().minusDays(21)
        );
    }

    @Test // WEEKLY ON DAYS во все дни, создана месяц назад, выполнена всегда
    void testReportStatsForWeeklyOnDaysEveryDayThatCreatedMonthAgoWithAllCompletions() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.WEEKLY_ON_DAYS;
        Set<DayOfWeek> daysOfWeek = Set.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
        );
        LocalDate createdAt = LocalDate.now().minusDays(30);

        when(reportRepository.countByHabitId(habitId)).thenReturn(31);

        when(reportRepository.existsByHabitIdAndDate(eq(habitId), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate date = invocation.getArgument(1);
                    return date.isBefore(LocalDate.now().plusDays(1)) &&
                            date.isAfter(LocalDate.now().minusDays(31));
                });

        List<Report> reports = IntStream.rangeClosed(0, 30)
                        .mapToObj(i -> Report.builder()
                                .date(LocalDate.now().minusDays(i))
                                .build())
                                .toList();

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(reports);

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                daysOfWeek,
                null,
                null,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(31);
        assertThat(response.getCompletionsPercent()).isEqualTo(100);
        assertThat(response.getSerialDays()).isEqualTo(31);
        assertThat(response.getCompletionsInPeriod()).isNull();
        assertThat(response.getCompletionsPlannedInPeriod()).isNull();
        List<LocalDate> reportDates = IntStream.rangeClosed(0, 30)
                .mapToObj(i -> LocalDate.now().minusDays(i))
                .toList();
        assertThat(response.getCompletedDays()).containsExactlyInAnyOrder(reportDates.toArray(LocalDate[]::new));
        assertThat(response.getUncompletedDays()).isEqualTo(List.of());
    }

    @Test // WEEKLY ON DAYS во все дни, создана полгода назад, выполнена всегда кроме вчера и сегодня
    void testReportStatsForWeeklyOnDaysEveryDayThatCreatedSixMonthsAgoWithAllCompletionsExceptTwo() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.WEEKLY_ON_DAYS;
        Set<DayOfWeek> daysOfWeek = Set.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
        );
        LocalDate createdAt = LocalDate.now().minusDays(180);

        when(reportRepository.countByHabitId(habitId)).thenReturn(179);

        when(reportRepository.existsByHabitIdAndDate(eq(habitId), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate date = invocation.getArgument(1);
                    return date.isBefore(LocalDate.now().minusDays(1)) &&
                            date.isAfter(LocalDate.now().minusDays(181));
                });

        List<Report> reports = IntStream.rangeClosed(2, 180)
                .mapToObj(i -> Report.builder()
                        .date(LocalDate.now().minusDays(i))
                        .build())
                .toList();

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(reports);

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                daysOfWeek,
                null,
                null,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(179);
        assertThat(response.getCompletionsPercent()).isEqualTo(98);
        assertThat(response.getSerialDays()).isEqualTo(0);
        assertThat(response.getCompletionsInPeriod()).isNull();
        assertThat(response.getCompletionsPlannedInPeriod()).isNull();
        List<LocalDate> reportDates = IntStream.rangeClosed(2, 180)
                .mapToObj(i -> LocalDate.now().minusDays(i))
                .toList();
        assertThat(response.getCompletedDays()).containsExactlyInAnyOrder(reportDates.toArray(LocalDate[]::new));
        assertThat(response.getUncompletedDays()).containsExactlyInAnyOrder(
                LocalDate.now(),
                LocalDate.now().minusDays(1)
        );
    }

    @Test // WEEKLY X TIMES 1 раз, создана сегодня, не выполнена ни разу
    void testReportStatsForWeeklyXTimesOneTimeThatCreatedTodayWithZeroCompletions() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.WEEKLY_X_TIMES;
        int timesPerWeek = 1;
        LocalDate createdAt = LocalDate.now();

        when(reportRepository.countByHabitId(habitId)).thenReturn(0);

        when(reportRepository.countByHabitIdAndDateBetween(
                habitId,
                LocalDate.now().with(DayOfWeek.MONDAY),
                LocalDate.now().with(DayOfWeek.MONDAY).plusDays(6)
        )).thenReturn(0);

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(List.of());

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                null,
                timesPerWeek,
                null,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(0);
        assertThat(response.getCompletionsPercent()).isNull();
        assertThat(response.getSerialDays()).isNull();
        assertThat(response.getCompletionsInPeriod()).isEqualTo(0);
        assertThat(response.getCompletionsPlannedInPeriod()).isEqualTo(1);
        assertThat(response.getCompletedDays()).isEqualTo(List.of());
        assertThat(response.getUncompletedDays()).isNull();
    }

    @Test // WEEKLY X TIMES 1 раз, создана сегодня, выполнена сегодня
    void testReportStatsForWeeklyXTimesOneTimeThatCreatedTodayWithOneCompletion() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.WEEKLY_X_TIMES;
        int timesPerWeek = 1;
        LocalDate createdAt = LocalDate.now();

        when(reportRepository.countByHabitId(habitId)).thenReturn(1);

        when(reportRepository.countByHabitIdAndDateBetween(
                habitId,
                LocalDate.now().with(DayOfWeek.MONDAY),
                LocalDate.now().with(DayOfWeek.MONDAY).plusDays(6)
        )).thenReturn(1);

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(List.of(
                Report.builder()
                        .date(LocalDate.now())
                        .build()
        ));

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                null,
                timesPerWeek,
                null,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(1);
        assertThat(response.getCompletionsPercent()).isNull();
        assertThat(response.getSerialDays()).isNull();
        assertThat(response.getCompletionsInPeriod()).isEqualTo(1);
        assertThat(response.getCompletionsPlannedInPeriod()).isEqualTo(1);
        assertThat(response.getCompletedDays()).isEqualTo(List.of(
                LocalDate.now()
        ));
        assertThat(response.getUncompletedDays()).isNull();
    }

    @Test // WEEKLY X TIMES 5 раз, создана месяц назад, выполнена 3 раза (из них на этой неделе 1)
    void testReportStatsForWeeklyXTimesFiveTimesThatCreatedMonthAgoWithThreeCompletions() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.WEEKLY_X_TIMES;
        int timesPerWeek = 5;
        LocalDate createdAt = LocalDate.now().minusDays(30);

        when(reportRepository.countByHabitId(habitId)).thenReturn(3);

        when(reportRepository.countByHabitIdAndDateBetween(
                habitId,
                LocalDate.now().with(DayOfWeek.MONDAY),
                LocalDate.now().with(DayOfWeek.MONDAY).plusDays(6)
        )).thenReturn(1);

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(List.of(
                Report.builder()
                        .date(LocalDate.now())
                        .build(),
                Report.builder()
                        .date(LocalDate.now().minusDays(7))
                        .build(),
                Report.builder()
                        .date(LocalDate.now().minusDays(8))
                        .build()
        ));

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                null,
                timesPerWeek,
                null,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(3);
        assertThat(response.getCompletionsPercent()).isNull();
        assertThat(response.getSerialDays()).isNull();
        assertThat(response.getCompletionsInPeriod()).isEqualTo(1);
        assertThat(response.getCompletionsPlannedInPeriod()).isEqualTo(5);
        assertThat(response.getCompletedDays()).containsExactlyInAnyOrder(
                LocalDate.now(),
                LocalDate.now().minusDays(7),
                LocalDate.now().minusDays(8)
        );
        assertThat(response.getUncompletedDays()).isNull();
    }

    @Test // WEEKLY X TIMES 7 раз, создана месяц назад, выполнена всегда
    void testReportStatsForWeeklyXTimesSevenTimesThatCreatedMonthAgoWithAllCompletions() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.WEEKLY_X_TIMES;
        int timesPerWeek = 7;
        LocalDate createdAt = LocalDate.now().minusDays(30);

        when(reportRepository.countByHabitId(habitId)).thenReturn(31);

        when(reportRepository.countByHabitIdAndDateBetween(
                habitId,
                LocalDate.now().with(DayOfWeek.MONDAY),
                LocalDate.now().with(DayOfWeek.MONDAY).plusDays(6)
        )).thenReturn(LocalDate.now().getDayOfWeek().getValue());

        List<Report> reports = IntStream.rangeClosed(0, 30)
                .mapToObj(i -> Report.builder()
                        .date(LocalDate.now().minusDays(i))
                        .build())
                .toList();

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(reports);

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                null,
                timesPerWeek,
                null,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(31);
        assertThat(response.getCompletionsPercent()).isNull();
        assertThat(response.getSerialDays()).isNull();
        assertThat(response.getCompletionsInPeriod()).isEqualTo(LocalDate.now().getDayOfWeek().getValue());
        assertThat(response.getCompletionsPlannedInPeriod()).isEqualTo(7);
        List<LocalDate> reportDates = IntStream.rangeClosed(0, 30)
                .mapToObj(i -> LocalDate.now().minusDays(i))
                .toList();
        assertThat(response.getCompletedDays()).containsExactlyInAnyOrder(reportDates.toArray(LocalDate[]::new));
        assertThat(response.getUncompletedDays()).isNull();
    }

    @Test // WEEKLY X TIMES 7 раз, создана полгода назад, выполнена всегда кроме вчера и сегодня
    void testReportStatsForWeeklyXTimesSevenTimesThatCreatedHalfYearAgoWithAllCompletionsExceptTwo() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.WEEKLY_X_TIMES;
        int timesPerWeek = 7;
        LocalDate createdAt = LocalDate.now().minusDays(180);

        when(reportRepository.countByHabitId(habitId)).thenReturn(179);

        int todayWeekDayNumber = LocalDate.now().getDayOfWeek().getValue();
        int countThisWeek = Math.max(todayWeekDayNumber - 2, 0);
        when(reportRepository.countByHabitIdAndDateBetween(
                habitId,
                LocalDate.now().with(DayOfWeek.MONDAY),
                LocalDate.now().with(DayOfWeek.MONDAY).plusDays(6)
        )).thenReturn(countThisWeek);

        List<Report> reports = IntStream.rangeClosed(2, 180)
                .mapToObj(i -> Report.builder()
                        .date(LocalDate.now().minusDays(i))
                        .build())
                .toList();

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(reports);

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                null,
                timesPerWeek,
                null,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(179);
        assertThat(response.getCompletionsPercent()).isNull();
        assertThat(response.getSerialDays()).isNull();
        assertThat(response.getCompletionsInPeriod()).isEqualTo(countThisWeek);
        assertThat(response.getCompletionsPlannedInPeriod()).isEqualTo(7);
        List<LocalDate> reportDates = IntStream.rangeClosed(2, 180)
                .mapToObj(i -> LocalDate.now().minusDays(i))
                .toList();
        assertThat(response.getCompletedDays()).containsExactlyInAnyOrder(reportDates.toArray(LocalDate[]::new));
        assertThat(response.getUncompletedDays()).isNull();
    }

    @Test // MONTHLY X TIMES 1 раз, создана сегодня, не выполнена ни разу
    void testReportStatsForMonthlyXTimesOneTimeThatCreatedTodayWithZeroCompletions() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.MONTHLY_X_TIMES;
        int timesPerMonth = 1;
        LocalDate createdAt = LocalDate.now();

        when(reportRepository.countByHabitId(habitId)).thenReturn(0);

        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        when(reportRepository.countByHabitIdAndDateBetween(
                habitId,
                startDate,
                endDate
        )).thenReturn(0);

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(List.of());

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                null,
                null,
                timesPerMonth,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(0);
        assertThat(response.getCompletionsPercent()).isNull();
        assertThat(response.getSerialDays()).isNull();
        assertThat(response.getCompletionsInPeriod()).isEqualTo(0);
        assertThat(response.getCompletionsPlannedInPeriod()).isEqualTo(1);
        assertThat(response.getCompletedDays()).isEqualTo(List.of());
        assertThat(response.getUncompletedDays()).isNull();
    }

    @Test // MONTHLY X TIMES 1 раз, создана сегодня, выполнена сегодня
    void testReportStatsForMonthlyXTimesOneTimeThatCreatedTodayWithOneCompletion() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.MONTHLY_X_TIMES;
        int timesPerMonth = 1;
        LocalDate createdAt = LocalDate.now();

        when(reportRepository.countByHabitId(habitId)).thenReturn(1);

        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        when(reportRepository.countByHabitIdAndDateBetween(
                habitId,
                startDate,
                endDate
        )).thenReturn(1);

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(List.of(
                Report.builder()
                        .date(LocalDate.now())
                        .build()
        ));

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                null,
                null,
                timesPerMonth,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(1);
        assertThat(response.getCompletionsPercent()).isNull();
        assertThat(response.getSerialDays()).isNull();
        assertThat(response.getCompletionsInPeriod()).isEqualTo(1);
        assertThat(response.getCompletionsPlannedInPeriod()).isEqualTo(1);
        assertThat(response.getCompletedDays()).isEqualTo(List.of(
                LocalDate.now()
        ));
        assertThat(response.getUncompletedDays()).isNull();
    }

    @Test // MONTHLY X TIMES 5 раз, создана месяц назад, выполнена 3 раза (и все в этом месяце)
    void testReportStatsForMonthlyXTimesFiveTimesThatCreatedMonthAgoWithThreeCompletions() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.MONTHLY_X_TIMES;
        int timesPerMonth = 5;
        LocalDate createdAt = LocalDate.now().minusDays(30);

        when(reportRepository.countByHabitId(habitId)).thenReturn(3);

        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        when(reportRepository.countByHabitIdAndDateBetween(
                habitId,
                startDate,
                endDate
        )).thenReturn(3);

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(List.of(
                Report.builder()
                        .date(LocalDate.now())
                        .build(),
                Report.builder()
                        .date(LocalDate.now().minusDays(3))
                        .build(),
                Report.builder()
                        .date(LocalDate.now().minusDays(5))
                        .build()
        ));

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                null,
                null,
                timesPerMonth,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(3);
        assertThat(response.getCompletionsPercent()).isNull();
        assertThat(response.getSerialDays()).isNull();
        assertThat(response.getCompletionsInPeriod()).isEqualTo(3);
        assertThat(response.getCompletionsPlannedInPeriod()).isEqualTo(5);
        assertThat(response.getCompletedDays()).isEqualTo(List.of(
                LocalDate.now(),
                LocalDate.now().minusDays(3),
                LocalDate.now().minusDays(5)
        ));
        assertThat(response.getUncompletedDays()).isNull();
    }

    @Test // MONTHLY X TIMES 30 раз, создана 29 дней назад, выполнена всегда
    void testReportStatsForMonthlyXTimes30TimesThatCreated29DaysAgoWithAllCompletions() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.MONTHLY_X_TIMES;
        int timesPerMonth = 30;
        LocalDate createdAt = LocalDate.now().minusDays(29);

        when(reportRepository.countByHabitId(habitId)).thenReturn(30);

        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        when(reportRepository.countByHabitIdAndDateBetween(
                habitId,
                startDate,
                endDate
        )).thenReturn(LocalDate.now().getDayOfMonth());

        List<Report> reports = IntStream.rangeClosed(0, 29)
                .mapToObj(i -> Report.builder()
                        .date(LocalDate.now().minusDays(i))
                        .build())
                .toList();

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(reports);

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                null,
                null,
                timesPerMonth,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(30);
        assertThat(response.getCompletionsPercent()).isNull();
        assertThat(response.getSerialDays()).isNull();
        assertThat(response.getCompletionsInPeriod()).isEqualTo(LocalDate.now().getDayOfMonth());
        assertThat(response.getCompletionsPlannedInPeriod()).isEqualTo(30);
        List<LocalDate> reportDates = IntStream.rangeClosed(0, 29)
                .mapToObj(i -> LocalDate.now().minusDays(i))
                .toList();
        assertThat(response.getCompletedDays()).containsExactlyInAnyOrder(reportDates.toArray(LocalDate[]::new));
        assertThat(response.getUncompletedDays()).isNull();
    }

    @Test // MONTHLY X TIMES 31 раз, создана 29 дней назад, выполнена всегда
    void testReportStatsForMonthlyXTimes31TimesThatCreated29DaysAgoWithAllCompletions() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.MONTHLY_X_TIMES;
        int timesPerMonth = 31;
        LocalDate createdAt = LocalDate.now().minusDays(29);

        when(reportRepository.countByHabitId(habitId)).thenReturn(30);

        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        when(reportRepository.countByHabitIdAndDateBetween(
                habitId,
                startDate,
                endDate
        )).thenReturn(LocalDate.now().getDayOfMonth());

        List<Report> reports = IntStream.rangeClosed(0, 29)
                .mapToObj(i -> Report.builder()
                        .date(LocalDate.now().minusDays(i))
                        .build())
                .toList();

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(reports);

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                null,
                null,
                timesPerMonth,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(30);
        assertThat(response.getCompletionsPercent()).isNull();
        assertThat(response.getSerialDays()).isNull();
        assertThat(response.getCompletionsInPeriod()).isEqualTo(LocalDate.now().getDayOfMonth());
        assertThat(response.getCompletionsPlannedInPeriod()).isEqualTo(31);
        List<LocalDate> reportDates = IntStream.rangeClosed(0, 29)
                .mapToObj(i -> LocalDate.now().minusDays(i))
                .toList();
        assertThat(response.getCompletedDays()).containsExactlyInAnyOrder(reportDates.toArray(LocalDate[]::new));
        assertThat(response.getUncompletedDays()).isNull();
    }

    @Test // MONTHLY X TIMES 31 раз, создана полгода назад, выполнена всегда кроме вчера и сегодня
    void testReportStatsForMonthlyXTimes31TimesThatCreatedHalfYearAgoWithAllCompletionsExceptTwo() {
        Long habitId = 10L;
        FrequencyType frequencyType = FrequencyType.MONTHLY_X_TIMES;
        int timesPerMonth = 31;
        LocalDate createdAt = LocalDate.now().minusDays(180);

        when(reportRepository.countByHabitId(habitId)).thenReturn(179);

        int todayMonthDayNumber = LocalDate.now().getDayOfMonth();
        int countThisMonth = Math.max(todayMonthDayNumber - 2, 0);
        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        when(reportRepository.countByHabitIdAndDateBetween(
                habitId,
                startDate,
                endDate
        )).thenReturn(countThisMonth);

        List<Report> reports = IntStream.rangeClosed(2, 180)
                .mapToObj(i -> Report.builder()
                        .date(LocalDate.now().minusDays(i))
                        .build())
                .toList();

        when(reportRepository.findAllByHabitId(habitId)).thenReturn(reports);

        ReportStatsResponse response = underTest.getReportStats(
                habitId,
                frequencyType,
                null,
                null,
                timesPerMonth,
                createdAt
        );

        assertThat(response.getCompletionsInTotal()).isEqualTo(179);
        assertThat(response.getCompletionsPercent()).isNull();
        assertThat(response.getSerialDays()).isNull();
        assertThat(response.getCompletionsInPeriod()).isEqualTo(countThisMonth);
        assertThat(response.getCompletionsPlannedInPeriod()).isEqualTo(31);
        List<LocalDate> reportDates = IntStream.rangeClosed(2, 180)
                .mapToObj(i -> LocalDate.now().minusDays(i))
                .toList();
        assertThat(response.getCompletedDays()).containsExactlyInAnyOrder(reportDates.toArray(LocalDate[]::new));
        assertThat(response.getUncompletedDays()).isNull();
    }

}