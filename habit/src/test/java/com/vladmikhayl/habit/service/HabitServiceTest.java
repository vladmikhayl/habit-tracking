package com.vladmikhayl.habit.service;

import com.vladmikhayl.commons.dto.HabitCreatedEvent;
import com.vladmikhayl.commons.dto.HabitDeletedEvent;
import com.vladmikhayl.habit.dto.request.HabitCreationRequest;
import com.vladmikhayl.habit.dto.request.HabitEditingRequest;
import com.vladmikhayl.habit.dto.response.*;
import com.vladmikhayl.habit.entity.Period;
import com.vladmikhayl.habit.entity.*;
import com.vladmikhayl.habit.repository.HabitRepository;
import com.vladmikhayl.habit.repository.SubscriptionCacheRepository;
import com.vladmikhayl.habit.service.feign.ReportClient;
import com.vladmikhayl.habit.service.kafka.HabitEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.vladmikhayl.habit.entity.FrequencyType.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

    // При тестировании методов getGeneralInfo() и getAllUserHabitsAtDay() предполагается, что сегодня 12 апреля 2025
    // Все тесты написаны исходя их этого предположения. Если поменять здесь эту дату, то тесты могут не работать
    private static final LocalDate TODAY_DATE = LocalDate.of(2025, 4, 12);

    @Value("${internal.token}")
    private String testInternalToken;

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private HabitEventProducer habitEventProducer;

    @Mock
    private ReportClient reportClient;

    @Mock
    private SubscriptionCacheRepository subscriptionCacheRepository;

    @Mock
    private Clock clock;

    // Этот InternalHabitService (он внедрится в HabitService) будет не моком,
    // а реально выполняющим логику сервисом с моком HabitRepository
    @Spy
    private InternalHabitService internalHabitService = new InternalHabitService(habitRepository);

    @InjectMocks
    private HabitService underTest;

    @BeforeEach
    void setUp() {
        // Указываем, что при вызове LocalDate.now(clock) в методах сервиса, нужно возвращать TODAY_DATE
        Instant fixedInstant = TODAY_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant();
        lenient().when(clock.instant()).thenReturn(fixedInstant);
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        // Вручную внедряем мок класса HabitRepository в internalHabitService
        // (так как по умолчанию в это поле кладется не мок репозитория, а null)
        ReflectionTestUtils.setField(internalHabitService, "habitRepository", habitRepository);
    }

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

        when(habitRepository.save(any(Habit.class))).thenReturn(
                Habit.builder()
                        .id(30L)
                        .isPhotoAllowed(false)
                        .name("Чистить зубы 2 раза в день")
                        .build()
        );

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
        assertThat(habitCreatedEventCaptured.isPhotoAllowed()).isEqualTo(false);
        assertThat(habitCreatedEventCaptured.habitName()).isEqualTo(request.getName());
    }

    @Test
    void canCreateHabitWithMaxInfo() {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Чистить зубы 2 раза в день")
                .description("Чистка зубов - это очень полезно")
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(60)
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        String userIdStr = "10";
        Long userId = 10L;

        when(habitRepository.existsByUserIdAndName(userId, request.getName())).thenReturn(false);

        when(habitRepository.save(any(Habit.class))).thenReturn(
                Habit.builder()
                        .id(30L)
                        .isPhotoAllowed(true)
                        .name("Чистить зубы 2 раза в день")
                        .build()
        );

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
                .frequencyType(WEEKLY_ON_DAYS)
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
        assertThat(habitCreatedEventCaptured.isPhotoAllowed()).isEqualTo(true);
        assertThat(habitCreatedEventCaptured.habitName()).isEqualTo(request.getName());
    }

    @Test
    void failCreateHabitWhenUserAlreadyHasHabitWithSameName() {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Чистить зубы 2 раза в день")
                .description(null)
                .isPhotoAllowed(false)
                .isHarmful(false)
                .durationDays(null)
                .frequencyType(WEEKLY_ON_DAYS)
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
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .build();

        HabitEditingRequest request = HabitEditingRequest.builder()
//                .name("Новое название привычки")
                .description("Новое описание привычки")
                .isHarmful(true)
                .durationDays(60)
                .build();

        String userIdStr = "1";
        Long userId = 1L;
        Long habitId = 2L;

        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(habit));
//        when(habitRepository.existsByUserIdAndName(userId, request.getName())).thenReturn(false);

        underTest.editHabit(habitId, request, userIdStr);

//        assertThat(habit.getName()).isEqualTo("Новое название привычки");
        assertThat(habit.getDescription()).isEqualTo("Новое описание привычки");
        assertThat(habit.isHarmful()).isTrue();
        assertThat(habit.getDurationDays()).isEqualTo(60);
    }

//    @Test
//    void canEditOnlyName() {
//        Habit habit = Habit.builder()
//                .name("Старое название привычки")
//                .description("Старое описание привычки")
//                .isHarmful(false)
//                .durationDays(30)
//                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
//                .timesPerWeek(3)
//                .build();
//
//        HabitEditingRequest request = HabitEditingRequest.builder()
//                .name("Новое название привычки")
//                .description(null)
//                .isHarmful(null)
//                .durationDays(null)
//                .build();
//
//        String userIdStr = "1";
//        Long userId = 1L;
//        Long habitId = 2L;
//
//        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(habit));
//
//        underTest.editHabit(habitId, request, userIdStr);
//
//        assertThat(habit.getName()).isEqualTo("Новое название привычки");
//        assertThat(habit.getDescription()).isEqualTo("Старое описание привычки");
//        assertThat(habit.isHarmful()).isFalse();
//        assertThat(habit.getDurationDays()).isEqualTo(30);
//    }

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
//                .name(null)
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
//                .name(null)
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
//                .name("Новое название привычки")
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

//    @Test
//    void failEditHabitWhenUserAlreadyHasHabitWithSameName() {
//        Habit habit = Habit.builder()
//                .name("Старое название привычки")
//                .description("Старое описание привычки")
//                .isHarmful(false)
//                .durationDays(30)
//                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
//                .timesPerWeek(3)
//                .build();
//
//        HabitEditingRequest request = HabitEditingRequest.builder()
//                .name("Новое название привычки")
//                .description("Новое описание привычки")
//                .isHarmful(true)
//                .durationDays(60)
//                .build();
//
//        String userIdStr = "1";
//        Long userId = 1L;
//        Long habitId = 2L;
//
//        when(habitRepository.findByIdAndUserId(habitId, userId)).thenReturn(Optional.of(habit));
//        when(habitRepository.existsByUserIdAndName(userId, request.getName())).thenReturn(true);
//
//        assertThatThrownBy(() -> underTest.editHabit(habitId, request, userIdStr))
//                .isInstanceOf(DataIntegrityViolationException.class)
//                .hasMessage("This user already has a habit with that name");
//    }

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
//                .name(null)
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
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(createdAt)
                .build();

        when(habitRepository.findById(habitId)).thenReturn(Optional.of(existingHabit));

        when(reportClient.getReportsInfo(
                testInternalToken,
                habitId,
                WEEKLY_ON_DAYS,
                Set.of(DayOfWeek.MONDAY),
                null,
                null,
                createdAt.toLocalDate()
        )).thenReturn(ResponseEntity.ok(HabitReportsInfoResponse.builder().build()));

        HabitReportsInfoResponse response = underTest.getReportsInfo(habitId, userIdStr);

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
                testInternalToken,
                habitId,
                FrequencyType.WEEKLY_X_TIMES,
                null,
                3,
                null,
                createdAt.toLocalDate()
        )).thenReturn(ResponseEntity.ok(HabitReportsInfoResponse.builder().build()));

        HabitReportsInfoResponse response = underTest.getReportsInfo(habitId, userIdStr);

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
                testInternalToken,
                habitId,
                FrequencyType.MONTHLY_X_TIMES,
                null,
                null,
                10,
                createdAt.toLocalDate()
        )).thenReturn(ResponseEntity.ok(HabitReportsInfoResponse.builder().build()));

        HabitReportsInfoResponse response = underTest.getReportsInfo(habitId, userIdStr);

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

        verify(reportClient, never()).getReportsInfo(any(), any(), any(), any(), any(), any(), any());
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

        when(reportClient.getReportAtDay(testInternalToken, habitId, date))
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

        when(reportClient.getReportAtDay(testInternalToken, habitId, date))
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

        verify(reportClient, never()).getReportAtDay(any(), any(), any());
    }

    @Test
    void canGetGeneralInfoWhenUserIsCreatorWithoutDuration() {
        String userIdStr = "10";
        Long userId = 10L;
        Long habitId = 52L;

        LocalDateTime createdAt = TODAY_DATE.atStartOfDay();

        when(habitRepository.existsByIdAndUserId(habitId, userId)).thenReturn(true);

        when(subscriptionCacheRepository.existsById(argThat(id ->
                id.getHabitId().equals(habitId) &&
                        id.getSubscriberId().equals(userId)
        ))).thenReturn(false);

        Habit existingHabit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .durationDays(null)
                .createdAt(createdAt)
                .build();

        when(habitRepository.findById(habitId)).thenReturn(Optional.of(existingHabit));

        when(subscriptionCacheRepository.countById_HabitId(habitId)).thenReturn(3);

        HabitGeneralInfoResponse response = underTest.getGeneralInfo(habitId, userIdStr);

        assertThat(response.getId()).isEqualTo(52L);
        assertThat(response.getDurationDays()).isNull();
        assertThat(response.getHowManyDaysLeft()).isNull();
        assertThat(response.getFrequencyType()).isEqualTo(WEEKLY_ON_DAYS);
        assertThat(response.getDaysOfWeek()).isEqualTo(Set.of(DayOfWeek.MONDAY));
        assertThat(response.getTimesPerWeek()).isNull();
        assertThat(response.getTimesPerMonth()).isNull();
        assertThat(response.getSubscribersCount()).isEqualTo(3);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void canGetGeneralInfoWhenUserIsCreatorWithDurationWhenHabitIsCreatedToday() {
        String userIdStr = "10";
        Long userId = 10L;
        Long habitId = 52L;

        LocalDateTime createdAt = TODAY_DATE.atStartOfDay();

        when(habitRepository.existsByIdAndUserId(habitId, userId)).thenReturn(true);

        when(subscriptionCacheRepository.existsById(argThat(id ->
                id.getHabitId().equals(habitId) &&
                        id.getSubscriberId().equals(userId)
        ))).thenReturn(false);

        Habit existingHabit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .durationDays(10)
                .createdAt(createdAt)
                .build();

        when(habitRepository.findById(habitId)).thenReturn(Optional.of(existingHabit));

        when(subscriptionCacheRepository.countById_HabitId(habitId)).thenReturn(0);

        HabitGeneralInfoResponse response = underTest.getGeneralInfo(habitId, userIdStr);

        assertThat(response.getId()).isEqualTo(52L);
        assertThat(response.getDurationDays()).isEqualTo(10);
        assertThat(response.getHowManyDaysLeft()).isEqualTo(10);
        assertThat(response.getFrequencyType()).isEqualTo(WEEKLY_ON_DAYS);
        assertThat(response.getDaysOfWeek()).isEqualTo(Set.of(DayOfWeek.MONDAY));
        assertThat(response.getTimesPerWeek()).isNull();
        assertThat(response.getTimesPerMonth()).isNull();
        assertThat(response.getSubscribersCount()).isEqualTo(0);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void canGetGeneralInfoWhenUserIsSubscriberWithDurationWhenTodayIsLastDay() {
        String userIdStr = "10";
        Long userId = 10L;
        Long habitId = 52L;

        LocalDateTime createdAt = TODAY_DATE.minusDays(2).atStartOfDay();

        when(habitRepository.existsByIdAndUserId(habitId, userId)).thenReturn(false);

        when(subscriptionCacheRepository.existsById(argThat(id ->
                id.getHabitId().equals(habitId) &&
                        id.getSubscriberId().equals(userId)
        ))).thenReturn(true);

        Habit existingHabit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .frequencyType(WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(1)
                .timesPerMonth(null)
                .durationDays(3)
                .createdAt(createdAt)
                .build();

        when(habitRepository.findById(habitId)).thenReturn(Optional.of(existingHabit));

        when(subscriptionCacheRepository.countById_HabitId(habitId)).thenReturn(10);

        HabitGeneralInfoResponse response = underTest.getGeneralInfo(habitId, userIdStr);

        assertThat(response.getId()).isEqualTo(52L);
        assertThat(response.getDurationDays()).isEqualTo(3);
        assertThat(response.getHowManyDaysLeft()).isEqualTo(1);
        assertThat(response.getFrequencyType()).isEqualTo(WEEKLY_X_TIMES);
        assertThat(response.getDaysOfWeek()).isNull();
        assertThat(response.getTimesPerWeek()).isEqualTo(1);
        assertThat(response.getTimesPerMonth()).isNull();
        assertThat(response.getSubscribersCount()).isEqualTo(10);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void canGetGeneralInfoWhenUserIsSubscriberWithDurationWhenHabitIsExpired() {
        String userIdStr = "10";
        Long userId = 10L;
        Long habitId = 52L;

        LocalDateTime createdAt = TODAY_DATE.minusDays(4).atStartOfDay();

        when(habitRepository.existsByIdAndUserId(habitId, userId)).thenReturn(false);

        when(subscriptionCacheRepository.existsById(argThat(id ->
                id.getHabitId().equals(habitId) &&
                        id.getSubscriberId().equals(userId)
        ))).thenReturn(true);

        Habit existingHabit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .frequencyType(MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(2)
                .durationDays(3)
                .createdAt(createdAt)
                .build();

        when(habitRepository.findById(habitId)).thenReturn(Optional.of(existingHabit));

        when(subscriptionCacheRepository.countById_HabitId(habitId)).thenReturn(10);

        HabitGeneralInfoResponse response = underTest.getGeneralInfo(habitId, userIdStr);

        assertThat(response.getId()).isEqualTo(52L);
        assertThat(response.getDurationDays()).isEqualTo(3);
        assertThat(response.getHowManyDaysLeft()).isEqualTo(-1);
        assertThat(response.getFrequencyType()).isEqualTo(MONTHLY_X_TIMES);
        assertThat(response.getDaysOfWeek()).isNull();
        assertThat(response.getTimesPerWeek()).isNull();
        assertThat(response.getTimesPerMonth()).isEqualTo(2);
        assertThat(response.getSubscribersCount()).isEqualTo(10);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void failGetGeneralInfoWhenUserIsNotCreatorAndIsNotSubscriber() {
        String userIdStr = "10";
        Long userId = 10L;
        Long habitId = 52L;

        when(habitRepository.existsByIdAndUserId(habitId, userId)).thenReturn(false);

        when(subscriptionCacheRepository.existsById(argThat(id ->
                id.getHabitId().equals(habitId) &&
                        id.getSubscriberId().equals(userId)
        ))).thenReturn(false);

        assertThatThrownBy(() -> underTest.getGeneralInfo(habitId, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .hasMessageContaining("This user doesn't have access to this habit");
    }

    @Test
    void testGetAllUserHabitsAtDayWhenThereAreNoHabits() {
        String userIdStr = "10";
        Long userId = 10L;

        when(habitRepository.findAllByUserId(userId)).thenReturn(List.of());

        List<HabitShortInfoResponse> response = underTest.getAllUserHabitsAtDay(TODAY_DATE, userIdStr);

        assertThat(response).isEqualTo(List.of());
    }

    @Test
    void testGetAllUserHabitsAtDayWhenThereAreNoCurrentHabits() {
        String userIdStr = "10";
        Long userId = 10L;

        // Эта привычка не будет текущей в TODAY_DATE (так как неподходящий день недели)
        Habit existingHabit1 = Habit.builder()
                .id(1L)
                .userId(userId)
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.FRIDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(TODAY_DATE.minusDays(3).atStartOfDay())
                .build();

        // Эта привычка не будет текущей в TODAY_DATE (так как истекла длительность)
        Habit existingHabit2 = Habit.builder()
                .id(2L)
                .userId(userId)
                .frequencyType(WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(3)
                .timesPerMonth(null)
                .durationDays(1)
                .createdAt(TODAY_DATE.minusDays(3).atStartOfDay())
                .build();

        when(habitRepository.findAllByUserId(userId)).thenReturn(List.of(existingHabit1, existingHabit2));

        when(habitRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(existingHabit1));

        when(habitRepository.findByIdAndUserId(2L, userId)).thenReturn(Optional.of(existingHabit2));

        List<HabitShortInfoResponse> response = underTest.getAllUserHabitsAtDay(TODAY_DATE, userIdStr);

        assertThat(response).isEqualTo(List.of());
    }

    @Test
    void testGetAllUserHabitsAtDayWhenThereIsOneCurrentHabit() {
        String userIdStr = "10";
        Long userId = 10L;

        // Эта привычка не будет текущей в TODAY_DATE (так как неподходящий день недели)
        Habit existingHabit1 = Habit.builder()
                .id(1L)
                .userId(userId)
                .name("Название 1")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.FRIDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(TODAY_DATE.minusDays(3).atStartOfDay())
                .build();

        // Эта привычка будет текущей в TODAY_DATE
        Habit existingHabit2 = Habit.builder()
                .id(2L)
                .userId(userId)
                .name("Название 2")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.SATURDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(TODAY_DATE.minusDays(3).atStartOfDay())
                .build();

        when(habitRepository.findAllByUserId(userId)).thenReturn(List.of(existingHabit1, existingHabit2));

        when(habitRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(existingHabit1));

        when(habitRepository.findByIdAndUserId(2L, userId)).thenReturn(Optional.of(existingHabit2));

        when(subscriptionCacheRepository.countById_HabitId(2L)).thenReturn(3);

        when(reportClient.isCompletedAtDay(testInternalToken, 2L, TODAY_DATE)).thenReturn(ResponseEntity.ok(
                ReportShortInfoResponse.builder()
                        .isCompleted(true)
                        .isPhotoUploaded(false)
                        .build()
        ));

        List<HabitShortInfoResponse> response = underTest.getAllUserHabitsAtDay(TODAY_DATE, userIdStr);

        assertThat(response).isEqualTo(List.of(
                HabitShortInfoResponse.builder()
                        .habitId(2L)
                        .name("Название 2")
                        .isCompleted(true)
                        .isPhotoAllowed(false)
                        .isPhotoUploaded(false)
                        .subscribersCount(3)
                        .frequencyType(WEEKLY_ON_DAYS)
                        .completionsInPeriod(null)
                        .completionsPlannedInPeriod(null)
                        .build()
        ));
    }

    @Test
    void testGetAllUserHabitsAtDayWhenThereAreMultipleCurrentHabits() {
        String userIdStr = "10";
        Long userId = 10L;

        // Эта привычка не будет текущей в TODAY_DATE (так как неподходящий день недели)
        Habit existingHabit1 = Habit.builder()
                .id(1L)
                .userId(userId)
                .name("Название 1")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.FRIDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(TODAY_DATE.minusDays(3).atStartOfDay())
                .build();

        // Эта привычка будет текущей в TODAY_DATE
        Habit existingHabit2 = Habit.builder()
                .id(2L)
                .userId(userId)
                .name("Название 2")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.SATURDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(TODAY_DATE.minusDays(3).atStartOfDay())
                .build();

        // Эта привычка будет текущей в TODAY_DATE
        Habit existingHabit3 = Habit.builder()
                .id(3L)
                .userId(userId)
                .name("Название 3")
                .frequencyType(WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(2)
                .timesPerMonth(null)
                .createdAt(TODAY_DATE.minusDays(3).atStartOfDay())
                .build();

        // Эта привычка будет текущей в TODAY_DATE
        Habit existingHabit4 = Habit.builder()
                .id(4L)
                .userId(userId)
                .name("Название 4")
                .frequencyType(MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(10)
                .isPhotoAllowed(true)
                .createdAt(TODAY_DATE.minusDays(3).atStartOfDay())
                .build();

        when(habitRepository.findAllByUserId(userId)).thenReturn(List.of(
                existingHabit1, existingHabit2, existingHabit3, existingHabit4
        ));

        when(habitRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(existingHabit1));

        when(habitRepository.findByIdAndUserId(2L, userId)).thenReturn(Optional.of(existingHabit2));

        when(habitRepository.findByIdAndUserId(3L, userId)).thenReturn(Optional.of(existingHabit3));

        when(habitRepository.findByIdAndUserId(4L, userId)).thenReturn(Optional.of(existingHabit4));

        when(subscriptionCacheRepository.countById_HabitId(2L)).thenReturn(3);

        when(reportClient.isCompletedAtDay(testInternalToken, 2L, TODAY_DATE)).thenReturn(ResponseEntity.ok(
                ReportShortInfoResponse.builder()
                        .isCompleted(true)
                        .isPhotoUploaded(false)
                        .build()
        ));

        when(subscriptionCacheRepository.countById_HabitId(3L)).thenReturn(0);

        when(reportClient.isCompletedAtDay(testInternalToken, 3L, TODAY_DATE)).thenReturn(ResponseEntity.ok(
                ReportShortInfoResponse.builder()
                        .isCompleted(false)
                        .isPhotoUploaded(false)
                        .build()
        ));

        when(reportClient.countCompletionsInPeriod(testInternalToken, 3L, Period.WEEK, TODAY_DATE)).thenReturn(ResponseEntity.ok(0));

        when(subscriptionCacheRepository.countById_HabitId(4L)).thenReturn(10);

        when(reportClient.isCompletedAtDay(testInternalToken, 4L, TODAY_DATE)).thenReturn(ResponseEntity.ok(
                ReportShortInfoResponse.builder()
                        .isCompleted(true)
                        .isPhotoUploaded(true)
                        .build()
        ));

        when(reportClient.countCompletionsInPeriod(testInternalToken, 4L, Period.MONTH, TODAY_DATE)).thenReturn(ResponseEntity.ok(3));

        List<HabitShortInfoResponse> response = underTest.getAllUserHabitsAtDay(TODAY_DATE, userIdStr);

        assertThat(response).isEqualTo(List.of(
                HabitShortInfoResponse.builder()
                        .habitId(2L)
                        .name("Название 2")
                        .isCompleted(true)
                        .subscribersCount(3)
                        .frequencyType(WEEKLY_ON_DAYS)
                        .completionsInPeriod(null)
                        .completionsPlannedInPeriod(null)
                        .isPhotoAllowed(false)
                        .isPhotoUploaded(false)
                        .build(),
                HabitShortInfoResponse.builder()
                        .habitId(3L)
                        .name("Название 3")
                        .isCompleted(false)
                        .subscribersCount(0)
                        .frequencyType(WEEKLY_X_TIMES)
                        .completionsInPeriod(0)
                        .completionsPlannedInPeriod(2)
                        .isPhotoAllowed(false)
                        .isPhotoUploaded(false)
                        .build(),
                HabitShortInfoResponse.builder()
                        .habitId(4L)
                        .name("Название 4")
                        .isCompleted(true)
                        .subscribersCount(10)
                        .frequencyType(MONTHLY_X_TIMES)
                        .completionsInPeriod(3)
                        .completionsPlannedInPeriod(10)
                        .isPhotoAllowed(true)
                        .isPhotoUploaded(true)
                        .build()
        ));
    }

    @Test
    void testGetAllUserSubscribedHabitsAtDayWhenThereAreNoSubscribedHabits() {
        String userIdStr = "10";
        Long userId = 10L;

        when(subscriptionCacheRepository.findAllById_SubscriberId(userId)).thenReturn(List.of());

        when(habitRepository.findAllByIdIn(List.of())).thenReturn(List.of());

        List<SubscribedHabitShortInfoResponse> response = underTest.getAllUserSubscribedHabitsAtDay(TODAY_DATE, userIdStr);

        assertThat(response).isEqualTo(List.of());

        verifyNoMoreInteractions(habitRepository, subscriptionCacheRepository);
    }

    @Test
    void testGetAllUserSubscribedHabitsAtDayWhenThereAreNoCurrentSubscribedHabits() {
        String userIdStr = "10";
        Long userId = 10L;

        // Текущий юзер подписан на привычку 1, которая не является текущий в TODAY_DATE
        SubscriptionCache existingSubscription = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(1L)
                        .subscriberId(userId)
                        .build())
                .creatorLogin("user2")
                .build();

        when(subscriptionCacheRepository.findAllById_SubscriberId(userId)).thenReturn(List.of(existingSubscription));

        // Эта привычка не будет текущей в TODAY_DATE (так как неподходящий день недели)
        Habit existingHabit = Habit.builder()
                .id(1L)
                .userId(2L)
                .name("Название 1")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.FRIDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(TODAY_DATE.minusDays(3).atStartOfDay())
                .build();

        when(habitRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(existingHabit));

        when(habitRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.of(existingHabit));

        List<SubscribedHabitShortInfoResponse> response = underTest.getAllUserSubscribedHabitsAtDay(TODAY_DATE, userIdStr);

        assertThat(response).isEqualTo(List.of());

        verifyNoMoreInteractions(habitRepository, subscriptionCacheRepository);
    }

    @Test
    void testGetAllUserSubscribedHabitsAtDayWhenThereAreCurrentWeeklyOnDaysSubscribedHabits() {
        String userIdStr = "10";
        Long userId = 10L;

        // Текущий юзер подписан на привычку 1, которая не является текущий в TODAY_DATE
        SubscriptionCache existingSubscription1 = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(1L)
                        .subscriberId(userId)
                        .build())
                .creatorLogin("user2")
                .build();

        // Текущий юзер подписан на привычку 2, которая является текущий в TODAY_DATE
        SubscriptionCache existingSubscription2 = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(2L)
                        .subscriberId(userId)
                        .build())
                .creatorLogin("user3")
                .build();

        when(subscriptionCacheRepository.findAllById_SubscriberId(userId)).thenReturn(List.of(
                existingSubscription1, existingSubscription2
        ));

        // Эта привычка не будет текущей в TODAY_DATE (так как истекла длительность)
        Habit existingHabit1 = Habit.builder()
                .id(1L)
                .userId(2L)
                .name("Название 1")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.SATURDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .durationDays(1)
                .createdAt(TODAY_DATE.minusDays(3).atStartOfDay())
                .build();

        // Эта привычка будет текущей в TODAY_DATE
        Habit existingHabit2 = Habit.builder()
                .id(2L)
                .userId(3L)
                .name("Название 2")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.SATURDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(TODAY_DATE.minusDays(3).atStartOfDay())
                .build();

        when(habitRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(
                existingHabit1, existingHabit2
        ));

        when(habitRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.of(existingHabit1));

        when(habitRepository.findByIdAndUserId(2L, 3L)).thenReturn(Optional.of(existingHabit2));

        when(subscriptionCacheRepository.findAllById_HabitId(2L)).thenReturn(List.of(existingSubscription2));

        when(subscriptionCacheRepository.countById_HabitId(2L)).thenReturn(1);

        when(reportClient.isCompletedAtDay(testInternalToken, 2L, TODAY_DATE)).thenReturn(ResponseEntity.ok(
                ReportShortInfoResponse
                        .builder()
                        .isCompleted(false)
                        .isPhotoUploaded(false)
                        .build()
        ));

        List<SubscribedHabitShortInfoResponse> response = underTest.getAllUserSubscribedHabitsAtDay(TODAY_DATE, userIdStr);

        assertThat(response).isEqualTo(List.of(
                SubscribedHabitShortInfoResponse.builder()
                        .habitId(2L)
                        .creatorLogin("user3")
                        .name("Название 2")
                        .isCompleted(false)
                        .subscribersCount(1)
                        .frequencyType(WEEKLY_ON_DAYS)
                        .completionsInPeriod(null)
                        .completionsPlannedInPeriod(null)
                        .isPhotoAllowed(false)
                        .isPhotoUploaded(false)
                        .build()
        ));

        verifyNoMoreInteractions(habitRepository, subscriptionCacheRepository, reportClient);
    }

    @Test
    void testGetAllUserSubscribedHabitsAtDayWhenThereAreCurrentWeeklyXTimesAndMonthlyXTimesSubscribedHabits() {
        String userIdStr = "10";
        Long userId = 10L;

        // Текущий юзер подписан на привычку 1, которая является текущий в TODAY_DATE
        SubscriptionCache existingSubscription1 = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(1L)
                        .subscriberId(userId)
                        .build())
                .creatorLogin("user2")
                .build();

        // Текущий юзер подписан на привычку 2, которая является текущий в TODAY_DATE
        SubscriptionCache existingSubscription2 = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(2L)
                        .subscriberId(userId)
                        .build())
                .creatorLogin("user2")
                .build();

        when(subscriptionCacheRepository.findAllById_SubscriberId(userId)).thenReturn(List.of(
                existingSubscription1, existingSubscription2
        ));

        // Эта привычка будет текущей в TODAY_DATE
        Habit existingHabit1 = Habit.builder()
                .id(1L)
                .userId(2L)
                .name("Название 1")
                .frequencyType(WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(5)
                .timesPerMonth(null)
                .createdAt(TODAY_DATE.minusDays(3).atStartOfDay())
                .build();

        // Эта привычка будет текущей в TODAY_DATE
        Habit existingHabit2 = Habit.builder()
                .id(2L)
                .userId(2L)
                .name("Название 2")
                .frequencyType(MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(10)
                .isPhotoAllowed(true)
                .createdAt(TODAY_DATE.minusDays(3).atStartOfDay())
                .build();

        when(habitRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(
                existingHabit1, existingHabit2
        ));

        // Стабы для привычки 1

        when(habitRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.of(existingHabit1));

        when(subscriptionCacheRepository.findAllById_HabitId(1L)).thenReturn(List.of(existingSubscription1));

        when(subscriptionCacheRepository.countById_HabitId(1L)).thenReturn(1);

        when(reportClient.isCompletedAtDay(testInternalToken, 1L, TODAY_DATE)).thenReturn(ResponseEntity.ok(
                ReportShortInfoResponse
                        .builder()
                        .isCompleted(false)
                        .isPhotoUploaded(false)
                        .build()
        ));

        when(reportClient.countCompletionsInPeriod(testInternalToken, 1L, Period.WEEK, TODAY_DATE)).thenReturn(ResponseEntity.ok(0));

        // Стабы для привычки 2

        when(habitRepository.findByIdAndUserId(2L, 2L)).thenReturn(Optional.of(existingHabit2));

        when(subscriptionCacheRepository.findAllById_HabitId(2L)).thenReturn(List.of(existingSubscription2));

        when(subscriptionCacheRepository.countById_HabitId(2L)).thenReturn(1);

        when(reportClient.isCompletedAtDay(testInternalToken, 2L, TODAY_DATE)).thenReturn(ResponseEntity.ok(
                ReportShortInfoResponse
                        .builder()
                        .isCompleted(true)
                        .isPhotoUploaded(true)
                        .build()
        ));

        when(reportClient.countCompletionsInPeriod(testInternalToken, 2L, Period.MONTH, TODAY_DATE)).thenReturn(ResponseEntity.ok(3));

        List<SubscribedHabitShortInfoResponse> response = underTest.getAllUserSubscribedHabitsAtDay(TODAY_DATE, userIdStr);

        assertThat(response).isEqualTo(List.of(
                SubscribedHabitShortInfoResponse.builder()
                        .habitId(1L)
                        .creatorLogin("user2")
                        .name("Название 1")
                        .isCompleted(false)
                        .subscribersCount(1)
                        .frequencyType(WEEKLY_X_TIMES)
                        .completionsInPeriod(0)
                        .completionsPlannedInPeriod(5)
                        .isPhotoAllowed(false)
                        .isPhotoUploaded(false)
                        .build(),
                SubscribedHabitShortInfoResponse.builder()
                        .habitId(2L)
                        .creatorLogin("user2")
                        .name("Название 2")
                        .isCompleted(true)
                        .subscribersCount(1)
                        .frequencyType(MONTHLY_X_TIMES)
                        .completionsInPeriod(3)
                        .completionsPlannedInPeriod(10)
                        .isPhotoAllowed(true)
                        .isPhotoUploaded(true)
                        .build()
        ));

        verifyNoMoreInteractions(habitRepository, subscriptionCacheRepository, reportClient);
    }

}