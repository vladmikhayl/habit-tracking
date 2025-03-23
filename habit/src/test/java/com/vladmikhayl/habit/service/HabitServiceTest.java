package com.vladmikhayl.habit.service;

import com.vladmikhayl.habit.dto.HabitCreationRequest;
import com.vladmikhayl.habit.entity.FrequencyType;
import com.vladmikhayl.habit.entity.Habit;
import com.vladmikhayl.habit.repository.HabitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;


import java.time.DayOfWeek;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

    @Mock
    private HabitRepository habitRepository;

    @InjectMocks
    private HabitService underTest;

    @Test
    void canCreateMinInfoHabit() {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Чистить зубы 2 раза в день")
                .description(null)
                .isPhotoAllowed(false)
                .isHarmful(false)
                .durationDays(null)
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(5)
                .timesPerMonth(null)
                .build();

        String userIdStr = "1";
        Long userId = 1L;

        when(habitRepository.existsByUserIdAndName(userId, request.getName())).thenReturn(false);

        underTest.createHabit(request, userIdStr);

        ArgumentCaptor<Habit> habitArgumentCaptor = ArgumentCaptor.forClass(Habit.class);

        verify(habitRepository).save(habitArgumentCaptor.capture());

        Habit capturedHabit = habitArgumentCaptor.getValue();

        Habit expected = Habit.builder()
                .userId(userId)
                .name("Чистить зубы 2 раза в день")
                .description(null)
                .isPhotoAllowed(false)
                .isHarmful(false)
                .durationDays(null)
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(5)
                .timesPerMonth(null)
                .build();

        assertThat(capturedHabit)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt")
                .isEqualTo(expected);
    }

    @Test
    void canCreateMaxInfoHabit() {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Чистить зубы 2 раза в день")
                .description("Чистка зубов - это очень полезно")
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(60)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        String userIdStr = "10";
        Long userId = 10L;

        when(habitRepository.existsByUserIdAndName(userId, request.getName())).thenReturn(false);

        underTest.createHabit(request, userIdStr);

        ArgumentCaptor<Habit> habitArgumentCaptor = ArgumentCaptor.forClass(Habit.class);

        verify(habitRepository).save(habitArgumentCaptor.capture());

        Habit capturedHabit = habitArgumentCaptor.getValue();

        Habit expected = Habit.builder()
                .userId(userId)
                .name("Чистить зубы 2 раза в день")
                .description("Чистка зубов - это очень полезно")
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(60)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        assertThat(capturedHabit)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt")
                .isEqualTo(expected);
    }

    @Test
    void failCreateHabitWithSameName() {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Чистить зубы 2 раза в день")
                .description(null)
                .isPhotoAllowed(false)
                .isHarmful(false)
                .durationDays(null)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        String userIdStr = "1";
        Long userId = 1L;

        when(habitRepository.existsByUserIdAndName(userId, request.getName())).thenReturn(true);

        assertThatThrownBy(() -> underTest.createHabit(request, userIdStr))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("This user already has a habit with that name");

        verify(habitRepository, never()).save(any());
    }

}