package com.vladmikhayl.habit.service;

import com.vladmikhayl.habit.entity.FrequencyType;
import com.vladmikhayl.habit.entity.Habit;
import com.vladmikhayl.habit.repository.HabitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalHabitServiceTest {

    @Mock
    private HabitRepository habitRepository;

    @InjectMocks
    private InternalHabitService underTest;

    @Test
    void testIsCurrentForWeeklyXTimesWithoutDurationWithCorrectDate() {
        Long habitId = 59L;
        Long userId = 12L;
        LocalDate date = LocalDate.of(2025, 4, 7);

        Habit existingHabit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .timesPerWeek(4)
                .durationDays(null)
                .createdAt(LocalDateTime.of(2025, 4, 7, 12, 30))
                .build();

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(existingHabit));

        boolean isCurrent = underTest.isCurrent(habitId, userId, date);
        assertThat(isCurrent).isTrue();
    }

    @Test
    void testIsCurrentForMonthlyXTimesWithoutDurationWithCorrectDate() {
        Long habitId = 59L;
        Long userId = 12L;
        LocalDate date = LocalDate.of(2025, 4, 10);

        Habit existingHabit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .frequencyType(FrequencyType.MONTHLY_X_TIMES)
                .timesPerMonth(10)
                .durationDays(null)
                .createdAt(LocalDateTime.of(2025, 4, 7, 12, 30))
                .build();

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(existingHabit));

        boolean isCurrent = underTest.isCurrent(habitId, userId, date);
        assertThat(isCurrent).isTrue();
    }

    @Test
    void testIsCurrentForWeeklyOnDaysWithoutDurationWithCorrectDay() {
        Long habitId = 59L;
        Long userId = 12L;
        LocalDate date = LocalDate.of(2025, 8, 10);

        Habit existingHabit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.SUNDAY))
                .durationDays(null)
                .createdAt(LocalDateTime.of(2025, 4, 7, 12, 30))
                .build();

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(existingHabit));

        boolean isCurrent = underTest.isCurrent(habitId, userId, date);
        assertThat(isCurrent).isTrue();
    }

    @Test
    void testIsCurrentForWeeklyOnDaysWithoutDurationWithIncorrectDay() {
        Long habitId = 59L;
        Long userId = 12L;
        LocalDate date = LocalDate.of(2025, 8, 9);

        Habit existingHabit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.SUNDAY))
                .durationDays(null)
                .createdAt(LocalDateTime.of(2025, 4, 7, 12, 30))
                .build();

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(existingHabit));

        boolean isCurrent = underTest.isCurrent(habitId, userId, date);
        assertThat(isCurrent).isFalse();
    }

    @Test
    void testIsCurrentWhenHabitDoesNotExist() {
        Long habitId = 59L;
        Long userId = 12L;
        LocalDate date = LocalDate.of(2025, 4, 9);

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.empty());

        boolean isCurrent = underTest.isCurrent(habitId, userId, date);
        assertThat(isCurrent).isFalse();
    }

    @Test
    void testIsCurrentForWeeklyXTimesWithoutDurationWhenItIsCreatedAfterDate() {
        Long habitId = 59L;
        Long userId = 12L;
        LocalDate date = LocalDate.of(2025, 4, 8);

        Habit existingHabit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .timesPerWeek(3)
                .durationDays(null)
                .createdAt(LocalDateTime.of(2025, 4, 9, 0, 0))
                .build();

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(existingHabit));

        boolean isCurrent = underTest.isCurrent(habitId, userId, date);
        assertThat(isCurrent).isFalse();
    }

    @Test
    void testIsCurrentForWeeklyOnDaysWhenItIsExpired() {
        Long habitId = 59L;
        Long userId = 12L;
        LocalDate date = LocalDate.of(2025, 4, 9);

        Habit existingHabit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.WEDNESDAY))
                .durationDays(14)
                .createdAt(LocalDateTime.of(2025, 3, 26, 12, 30))
                .build();

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(existingHabit));

        boolean isCurrent = underTest.isCurrent(habitId, userId, date);
        assertThat(isCurrent).isFalse();
    }

    @Test
    void testIsCurrentForWeeklyOnDaysWhenItIsLastDay() {
        Long habitId = 59L;
        Long userId = 12L;
        LocalDate date = LocalDate.of(2025, 4, 9);

        Habit existingHabit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.WEDNESDAY))
                .durationDays(14)
                .createdAt(LocalDateTime.of(2025, 3, 27, 12, 30))
                .build();

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(existingHabit));

        boolean isCurrent = underTest.isCurrent(habitId, userId, date);
        assertThat(isCurrent).isTrue();
    }

}