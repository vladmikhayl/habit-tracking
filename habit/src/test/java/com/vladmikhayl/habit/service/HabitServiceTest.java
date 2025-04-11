package com.vladmikhayl.habit.service;

import com.vladmikhayl.habit.dto.request.HabitCreationRequest;
import com.vladmikhayl.habit.dto.request.HabitEditingRequest;
import com.vladmikhayl.habit.dto.event.HabitCreatedEvent;
import com.vladmikhayl.habit.dto.event.HabitDeletedEvent;
import com.vladmikhayl.habit.dto.response.ReportFullInfoResponse;
import com.vladmikhayl.habit.dto.response.ReportStatsResponse;
import com.vladmikhayl.habit.entity.FrequencyType;
import com.vladmikhayl.habit.entity.Habit;
import com.vladmikhayl.habit.repository.HabitRepository;
import com.vladmikhayl.habit.repository.SubscriptionCacheRepository;
import com.vladmikhayl.habit.service.feign.ReportClient;
import com.vladmikhayl.habit.service.kafka.HabitEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private HabitEventProducer habitEventProducer;

    @Mock
    private ReportClient reportClient;

    @Mock
    private SubscriptionCacheRepository subscriptionCacheRepository;

    @InjectMocks
    private HabitService underTest;

    @Test
    void canCreateHabitWithMinInfo() {
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

        when(habitRepository.save(any(Habit.class))).thenReturn(Habit.builder().id(30L).build());

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

        ArgumentCaptor<HabitCreatedEvent> habitCreatedEventArgumentCaptor =
                ArgumentCaptor.forClass(HabitCreatedEvent.class);

        verify(habitEventProducer).sendHabitCreatedEvent(habitCreatedEventArgumentCaptor.capture());

        HabitCreatedEvent habitCreatedEventCaptured = habitCreatedEventArgumentCaptor.getValue();

        assertThat(habitCreatedEventCaptured.habitId()).isEqualTo(30L);
    }

    @Test
    void canCreateHabitWithMaxInfo() {
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

        when(habitRepository.save(any(Habit.class))).thenReturn(Habit.builder().id(30L).build());

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

        ArgumentCaptor<HabitCreatedEvent> habitCreatedEventArgumentCaptor =
                ArgumentCaptor.forClass(HabitCreatedEvent.class);

        verify(habitEventProducer).sendHabitCreatedEvent(habitCreatedEventArgumentCaptor.capture());

        HabitCreatedEvent habitCreatedEventCaptured = habitCreatedEventArgumentCaptor.getValue();

        assertThat(habitCreatedEventCaptured.habitId()).isEqualTo(30L);
    }

    @Test
    void failCreateHabitWhenUserAlreadyHasHabitWithSameName() {
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

        verify(habitEventProducer, never()).sendHabitCreatedEvent(any());
    }

    @Test
    void canMaxEditHabit() {
        Habit habit = Habit.builder()
                .name("Старое название привычки")
                .description("Старое описание привычки")
                .isHarmful(false)
                .durationDays(null)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .build();

        HabitEditingRequest request = HabitEditingRequest.builder()
                .name("Новое название привычки")
                .description("Новое описание привычки")
                .isHarmful(true)
                .durationDays(60)
                .build();

        String userIdStr = "1";
        Long userId = 1L;
        Long habitId = 2L;

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(habit));
        when(habitRepository.existsByUserIdAndName(userId, request.getName())).thenReturn(false);

        underTest.editHabit(habitId, request, userIdStr);

        assertThat(habit.getName()).isEqualTo("Новое название привычки");
        assertThat(habit.getDescription()).isEqualTo("Новое описание привычки");
        assertThat(habit.isHarmful()).isTrue();
        assertThat(habit.getDurationDays()).isEqualTo(60);
    }

    @Test
    void canEditOnlyName() {
        Habit habit = Habit.builder()
                .name("Старое название привычки")
                .description("Старое описание привычки")
                .isHarmful(false)
                .durationDays(30)
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .timesPerWeek(3)
                .build();

        HabitEditingRequest request = HabitEditingRequest.builder()
                .name("Новое название привычки")
                .description(null)
                .isHarmful(null)
                .durationDays(null)
                .build();

        String userIdStr = "1";
        Long userId = 1L;
        Long habitId = 2L;

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(habit));

        underTest.editHabit(habitId, request, userIdStr);

        assertThat(habit.getName()).isEqualTo("Новое название привычки");
        assertThat(habit.getDescription()).isEqualTo("Старое описание привычки");
        assertThat(habit.isHarmful()).isFalse();
        assertThat(habit.getDurationDays()).isEqualTo(30);
    }

    @Test
    void canNullEditHabit() {
        Habit habit = Habit.builder()
                .name("Старое название привычки")
                .description("Старое описание привычки")
                .isHarmful(false)
                .durationDays(30)
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .timesPerWeek(3)
                .build();

        HabitEditingRequest request = HabitEditingRequest.builder()
                .name(null)
                .description(null)
                .isHarmful(null)
                .durationDays(null)
                .build();

        String userIdStr = "1";
        Long userId = 1L;
        Long habitId = 2L;

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(habit));

        underTest.editHabit(habitId, request, userIdStr);

        assertThat(habit.getName()).isEqualTo("Старое название привычки");
        assertThat(habit.getDescription()).isEqualTo("Старое описание привычки");
        assertThat(habit.isHarmful()).isFalse();
        assertThat(habit.getDurationDays()).isEqualTo(30);
    }

    @Test
    void canSetDurationDaysToNull() {
        Habit habit = Habit.builder()
                .name("Старое название привычки")
                .description("Старое описание привычки")
                .isHarmful(false)
                .durationDays(30)
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .timesPerWeek(3)
                .build();

        HabitEditingRequest request = HabitEditingRequest.builder()
                .name(null)
                .description(null)
                .isHarmful(null)
                .durationDays(0)
                .build();

        String userIdStr = "1";
        Long userId = 1L;
        Long habitId = 2L;

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(habit));

        underTest.editHabit(habitId, request, userIdStr);

        assertThat(habit.getName()).isEqualTo("Старое название привычки");
        assertThat(habit.getDescription()).isEqualTo("Старое описание привычки");
        assertThat(habit.isHarmful()).isFalse();
        assertThat(habit.getDurationDays()).isNull();
    }

    @Test
    void failEditHabitWhenUserDoesNotHaveThisHabit() {
        HabitEditingRequest request = HabitEditingRequest.builder()
                .name("Новое название привычки")
                .description("Новое описание привычки")
                .isHarmful(true)
                .durationDays(60)
                .build();

        String userIdStr = "1";
        Long userId = 1L;
        Long habitId = 2L;

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.editHabit(habitId, request, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .hasMessageContaining("This user doesn't have a habit with this id");
    }

    @Test
    void failEditHabitWhenUserAlreadyHasHabitWithSameName() {
        Habit habit = Habit.builder()
                .name("Старое название привычки")
                .description("Старое описание привычки")
                .isHarmful(false)
                .durationDays(30)
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .timesPerWeek(3)
                .build();

        HabitEditingRequest request = HabitEditingRequest.builder()
                .name("Новое название привычки")
                .description("Новое описание привычки")
                .isHarmful(true)
                .durationDays(60)
                .build();

        String userIdStr = "1";
        Long userId = 1L;
        Long habitId = 2L;

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(habit));
        when(habitRepository.existsByUserIdAndName(userId, request.getName())).thenReturn(true);

        assertThatThrownBy(() -> underTest.editHabit(habitId, request, userIdStr))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("This user already has a habit with that name");
    }

    @Test
    void failSetHarmfulTrueWithNotWeeklyOnDaysFrequency() {
        Habit habit = Habit.builder()
                .name("Старое название привычки")
                .description("Старое описание привычки")
                .isHarmful(false)
                .durationDays(30)
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .timesPerWeek(3)
                .build();

        HabitEditingRequest request = HabitEditingRequest.builder()
                .name(null)
                .description(null)
                .isHarmful(true)
                .durationDays(null)
                .build();

        String userIdStr = "1";
        Long userId = 1L;
        Long habitId = 2L;

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(habit));

        assertThatThrownBy(() -> underTest.editHabit(habitId, request, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                })
                .hasMessageContaining("A habit with this FrequencyType cannot be harmful");
    }

    @Test
    void canDeleteHabitWhenItBelongsToUser() {
        String userIdStr = "10";
        Long userId = 10L;
        Long habitId = 52L;

        Habit habit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .build();

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(habit));

        underTest.deleteHabit(habitId, userIdStr);

        ArgumentCaptor<Habit> habitArgumentCaptor = ArgumentCaptor.forClass(Habit.class);

        verify(habitRepository).delete(habitArgumentCaptor.capture());

        Habit capturedHabit = habitArgumentCaptor.getValue();

        assertThat(capturedHabit.getId()).isEqualTo(52L);

        ArgumentCaptor<HabitDeletedEvent> habitDeletedEventArgumentCaptor =
                ArgumentCaptor.forClass(HabitDeletedEvent.class);

        verify(habitEventProducer).sendHabitDeletedEvent(habitDeletedEventArgumentCaptor.capture());

        HabitDeletedEvent habitDeletedEventCaptured = habitDeletedEventArgumentCaptor.getValue();

        assertThat(habitDeletedEventCaptured.habitId()).isEqualTo(52L);
    }

    @Test
    void failDeleteHabitWhenItDoesNotBelongToUser() {
        String userIdStr = "10";
        Long userId = 10L;
        Long habitId = 52L;

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.deleteHabit(habitId, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .hasMessageContaining("This user doesn't have a habit with this id");

        verify(habitEventProducer, never()).sendHabitDeletedEvent(any());
    }

    @Test
    void canGetReportsInfoForWeeklyOnDaysWhenUserIsCreator() {
        String userIdStr = "10";
        Long userId = 10L;
        Long habitId = 52L;

        LocalDateTime createdAt = LocalDateTime.of(2025, 4, 10, 12, 40);

        when(habitRepository.existsByIdAndUserId(habitId, userId)).thenReturn(true);

        when(subscriptionCacheRepository.existsById(argThat(id ->
                id.getHabitId().equals(habitId) &&
                        id.getSubscriberId().equals(userId)
        ))).thenReturn(false);

        Habit existingHabit = Habit.builder()
                .id(habitId)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(createdAt)
                .build();

        when(habitRepository.findById(habitId)).thenReturn(Optional.of(existingHabit));

        when(reportClient.getReportsInfo(
                habitId,
                FrequencyType.WEEKLY_ON_DAYS,
                Set.of(DayOfWeek.MONDAY),
                null,
                null,
                createdAt.toLocalDate()
        )).thenReturn(ResponseEntity.ok(ReportStatsResponse.builder().build()));

        ReportStatsResponse response = underTest.getReportsInfo(habitId, userIdStr);

        assertThat(response).isNotNull();
    }

    @Test
    void canGetReportsInfoForWeeklyXTimesWhenUserIsCreator() {
        String userIdStr = "10";
        Long userId = 10L;
        Long habitId = 52L;

        LocalDateTime createdAt = LocalDateTime.of(2025, 4, 10, 12, 40);

        when(habitRepository.existsByIdAndUserId(habitId, userId)).thenReturn(true);

        when(subscriptionCacheRepository.existsById(argThat(id ->
                id.getHabitId().equals(habitId) &&
                        id.getSubscriberId().equals(userId)
        ))).thenReturn(false);

        Habit existingHabit = Habit.builder()
                .id(habitId)
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .daysOfWeek(Set.of())
                .timesPerWeek(3)
                .timesPerMonth(null)
                .createdAt(createdAt)
                .build();

        when(habitRepository.findById(habitId)).thenReturn(Optional.of(existingHabit));

        when(reportClient.getReportsInfo(
                habitId,
                FrequencyType.WEEKLY_X_TIMES,
                null,
                3,
                null,
                createdAt.toLocalDate()
        )).thenReturn(ResponseEntity.ok(ReportStatsResponse.builder().build()));

        ReportStatsResponse response = underTest.getReportsInfo(habitId, userIdStr);

        assertThat(response).isNotNull();
    }

    @Test
    void canGetReportsInfoForMonthlyXTimesWhenUserIsSubscriber() {
        String userIdStr = "10";
        Long userId = 10L;
        Long habitId = 52L;

        LocalDateTime createdAt = LocalDateTime.of(2025, 4, 10, 12, 40);

        when(habitRepository.existsByIdAndUserId(habitId, userId)).thenReturn(false);

        when(subscriptionCacheRepository.existsById(argThat(id ->
                id.getHabitId().equals(habitId) &&
                        id.getSubscriberId().equals(userId)
        ))).thenReturn(true);

        Habit existingHabit = Habit.builder()
                .id(habitId)
                .frequencyType(FrequencyType.MONTHLY_X_TIMES)
                .daysOfWeek(Set.of())
                .timesPerWeek(null)
                .timesPerMonth(10)
                .createdAt(createdAt)
                .build();

        when(habitRepository.findById(habitId)).thenReturn(Optional.of(existingHabit));

        when(reportClient.getReportsInfo(
                habitId,
                FrequencyType.MONTHLY_X_TIMES,
                null,
                null,
                10,
                createdAt.toLocalDate()
        )).thenReturn(ResponseEntity.ok(ReportStatsResponse.builder().build()));

        ReportStatsResponse response = underTest.getReportsInfo(habitId, userIdStr);

        assertThat(response).isNotNull();
    }

    @Test
    void failGetReportsInfoWhenUserIsNotCreatorAndIsNotSubscriber() {
        String userIdStr = "10";
        Long userId = 10L;
        Long habitId = 52L;

        when(habitRepository.existsByIdAndUserId(habitId, userId)).thenReturn(false);

        when(subscriptionCacheRepository.existsById(argThat(id ->
                id.getHabitId().equals(habitId) &&
                        id.getSubscriberId().equals(userId)
        ))).thenReturn(false);

        assertThatThrownBy(() -> underTest.getReportsInfo(habitId, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .hasMessageContaining("This user doesn't have access to this habit");

        verify(reportClient, never()).getReportsInfo(any(), any(), any(), any(), any(), any());
    }

    @Test
    void canGetReportAtDayWhenUserIsCreator() {
        String userIdStr = "10";
        Long userId = 10L;
        Long habitId = 52L;

        LocalDate date = LocalDate.of(2025, 4, 10);

        when(habitRepository.existsByIdAndUserId(habitId, userId)).thenReturn(true);

        when(subscriptionCacheRepository.existsById(argThat(id ->
                id.getHabitId().equals(habitId) &&
                        id.getSubscriberId().equals(userId)
        ))).thenReturn(false);

        when(reportClient.getReportAtDay(habitId,date))
                .thenReturn(ResponseEntity.ok(ReportFullInfoResponse.builder().build()));

        ReportFullInfoResponse response = underTest.getReportAtDay(habitId, date, userIdStr);

        assertThat(response).isNotNull();
    }

    @Test
    void canGetReportAtDayWhenUserIsSubscriber() {
        String userIdStr = "10";
        Long userId = 10L;
        Long habitId = 52L;

        LocalDate date = LocalDate.of(2025, 4, 10);

        when(habitRepository.existsByIdAndUserId(habitId, userId)).thenReturn(false);

        when(subscriptionCacheRepository.existsById(argThat(id ->
                id.getHabitId().equals(habitId) &&
                        id.getSubscriberId().equals(userId)
        ))).thenReturn(true);

        when(reportClient.getReportAtDay(habitId,date))
                .thenReturn(ResponseEntity.ok(ReportFullInfoResponse.builder().build()));

        ReportFullInfoResponse response = underTest.getReportAtDay(habitId, date, userIdStr);

        assertThat(response).isNotNull();
    }

    @Test
    void failGetReportAtDayWhenUserIsNotCreatorAndIsNotSubscriber() {
        String userIdStr = "10";
        Long userId = 10L;
        Long habitId = 52L;

        LocalDate date = LocalDate.of(2025, 4, 10);

        when(habitRepository.existsByIdAndUserId(habitId, userId)).thenReturn(false);

        when(subscriptionCacheRepository.existsById(argThat(id ->
                id.getHabitId().equals(habitId) &&
                        id.getSubscriberId().equals(userId)
        ))).thenReturn(false);

        assertThatThrownBy(() -> underTest.getReportAtDay(habitId, date, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .hasMessageContaining("This user doesn't have access to this habit");

        verify(reportClient, never()).getReportAtDay(any(), any());
    }

}