package com.vladmikhayl.habit.service;

import com.vladmikhayl.habit.dto.request.HabitCreationRequest;
import com.vladmikhayl.habit.dto.request.HabitEditingRequest;
import com.vladmikhayl.habit.dto.event.HabitCreatedEvent;
import com.vladmikhayl.habit.dto.event.HabitDeletedEvent;
import com.vladmikhayl.habit.dto.response.*;
import com.vladmikhayl.habit.entity.*;
import com.vladmikhayl.habit.repository.HabitRepository;
import com.vladmikhayl.habit.repository.SubscriptionCacheRepository;
import com.vladmikhayl.habit.service.feign.ReportClient;
import com.vladmikhayl.habit.service.kafka.HabitEventProducer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepository habitRepository;

    private final HabitEventProducer habitEventProducer;

    private final SubscriptionCacheRepository subscriptionCacheRepository;

    private final ReportClient reportClient;

    private final Clock clock;

    private final InternalHabitService internalHabitService;

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid user ID format");
        }
    }

    public void createHabit(
            HabitCreationRequest request,
            String userId
    ) {
        Long userIdLong = parseUserId(userId);

        if (habitRepository.existsByUserIdAndName(userIdLong, request.getName())) {
            throw new DataIntegrityViolationException("This user already has a habit with that name");
        }

        Habit habit = Habit.builder()
                .userId(userIdLong)
                .name(request.getName())
                .description(request.getDescription())
                .isPhotoAllowed(request.isPhotoAllowed())
                .isHarmful(request.isHarmful())
                .durationDays(request.getDurationDays())
                .frequencyType(request.getFrequencyType())
                .daysOfWeek(request.getDaysOfWeek())
                .timesPerWeek(request.getTimesPerWeek())
                .timesPerMonth(request.getTimesPerMonth())
                .build();

        Habit savedHabit = habitRepository.save(habit);

        // Отправка события о создании привычки всем, кто подписан на habit-created
        HabitCreatedEvent event = HabitCreatedEvent.builder()
                .habitId(savedHabit.getId())
                .userId(savedHabit.getUserId())
                .isPhotoAllowed(savedHabit.isPhotoAllowed())
                .build();
        habitEventProducer.sendHabitCreatedEvent(event);
    }

    @Transactional
    public void editHabit(
            Long habitId,
            HabitEditingRequest request,
            String userId
    ) {
        Long userIdLong = parseUserId(userId);

        Habit habit = habitRepository.findByIdAndUserId(habitId, userIdLong)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "This user doesn't have a habit with this id"));

        if (request.getName() != null) {
            if (habitRepository.existsByUserIdAndName(userIdLong, request.getName())) {
                throw new DataIntegrityViolationException("This user already has a habit with that name");
            }
            habit.setName(request.getName());
        }

        if (request.getDescription() != null) {
            habit.setDescription(request.getDescription());
        }

        if (request.getIsHarmful() != null) {
            if (habit.getFrequencyType() != FrequencyType.WEEKLY_ON_DAYS && request.getIsHarmful()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A habit with this FrequencyType cannot be harmful");
            }
            habit.setHarmful(request.getIsHarmful());
        }

        // Чтобы положить значение null в поле durationDays, в качестве этого параметра нужно передать 0
        if (request.getDurationDays() != null) {
            if (request.getDurationDays() == 0) {
                habit.setDurationDays(null);
            } else {
                habit.setDurationDays(request.getDurationDays());
            }
        }
    }

    public void deleteHabit(Long habitId, String userId) {
        Long userIdLong = parseUserId(userId);

        Habit habit = habitRepository.findByIdAndUserId(habitId, userIdLong)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "This user doesn't have a habit with this id"));

        habitRepository.delete(habit);

        // Отправка события об удалении привычки всем, кто подписан на habit-deleted
        HabitDeletedEvent event = HabitDeletedEvent.builder()
                .habitId(habitId)
                .build();
        habitEventProducer.sendHabitDeletedEvent(event);
    }

    public HabitGeneralInfoResponse getGeneralInfo(Long habitId, String userId) {
        Long userIdLong = parseUserId(userId);

        boolean doesUserHaveAccess = isUserEitherHabitCreatorOrSubscriber(habitId, userIdLong);

        if (!doesUserHaveAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This user doesn't have access to this habit");
        }

        Habit habit = habitRepository.findById(habitId).get();

        int subscribersCount = subscriptionCacheRepository.countById_HabitId(habitId);

        Integer howManyDaysLeft = null;

        if (habit.getDurationDays() != null) {
            // Сколько полных дней прошло с момента создания привычки (то есть исключая текущий день)
            int howManyFullDaysPassed = (int) ChronoUnit.DAYS.between(
                    habit.getCreatedAt().toLocalDate(),
                    LocalDate.now(clock)
            );

            howManyDaysLeft = habit.getDurationDays() - howManyFullDaysPassed;
        }

        Set<DayOfWeek> daysOfWeek = habit.getDaysOfWeek();

        return HabitGeneralInfoResponse.builder()
                .id(habit.getId())
                .name(habit.getName())
                .description(habit.getDescription())
                .isPhotoAllowed(habit.isPhotoAllowed())
                .isHarmful(habit.isHarmful())
                .durationDays(habit.getDurationDays())
                .howManyDaysLeft(howManyDaysLeft)
                .frequencyType(habit.getFrequencyType())
                .daysOfWeek(daysOfWeek == null || daysOfWeek.isEmpty() ? null : daysOfWeek)
                .timesPerWeek(habit.getTimesPerWeek())
                .timesPerMonth(habit.getTimesPerMonth())
                .createdAt(habit.getCreatedAt())
                .subscribersCount(subscribersCount)
                .build();
    }

    public HabitReportsInfoResponse getReportsInfo(Long habitId, String userId) {
        Long userIdLong = parseUserId(userId);

        boolean doesUserHaveAccess = isUserEitherHabitCreatorOrSubscriber(habitId, userIdLong);

        if (!doesUserHaveAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This user doesn't have access to this habit");
        }

        Optional<Habit> habit = habitRepository.findById(habitId);

        Set<DayOfWeek> daysOfWeek = habit.get().getDaysOfWeek();

        return reportClient.getReportsInfo(
                habitId,
                habit.get().getFrequencyType(),
                daysOfWeek == null || daysOfWeek.isEmpty() ? null : daysOfWeek,
                habit.get().getTimesPerWeek(),
                habit.get().getTimesPerMonth(),
                habit.get().getCreatedAt().toLocalDate()
        ).getBody();
    }

    public ReportFullInfoResponse getReportAtDay(Long habitId, LocalDate date, String userId) {
        Long userIdLong = parseUserId(userId);

        boolean doesUserHaveAccess = isUserEitherHabitCreatorOrSubscriber(habitId, userIdLong);

        if (!doesUserHaveAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This user doesn't have access to this habit");
        }

        return reportClient.getReportAtDay(habitId, date).getBody();
    }

    public List<HabitShortInfoResponse> getAllUserHabitsAtDay(LocalDate date, String userId) {
        Long userIdLong = parseUserId(userId);
        
        List<Habit> allUserHabits = habitRepository.findAllByUserId(userIdLong);

        List<HabitShortInfoResponse> currentUserHabitsAtDay = new ArrayList<>();

        for (Habit habit : allUserHabits) {

            Long habitId = habit.getId();

            if (internalHabitService.isCurrent(habitId, userIdLong, date)) {

                FrequencyType frequencyType = habit.getFrequencyType();

                int subscribersCount = subscriptionCacheRepository.countById_HabitId(habitId);

                boolean isCompleted = reportClient.isCompletedAtDay(habitId, date).getBody();

                Integer completionsInPeriod = null;

                if (frequencyType == FrequencyType.WEEKLY_X_TIMES) {
                    completionsInPeriod = reportClient.countCompletionsInPeriod(habitId, Period.WEEK, date).getBody();
                }

                if (frequencyType == FrequencyType.MONTHLY_X_TIMES) {
                    completionsInPeriod = reportClient.countCompletionsInPeriod(habitId, Period.MONTH, date).getBody();
                }

                Integer completionsPlannedInPeriod = null;

                if (frequencyType == FrequencyType.WEEKLY_X_TIMES) {
                    completionsPlannedInPeriod = habit.getTimesPerWeek();
                }

                if (frequencyType == FrequencyType.MONTHLY_X_TIMES) {
                    completionsPlannedInPeriod = habit.getTimesPerMonth();
                }

                currentUserHabitsAtDay.add(HabitShortInfoResponse.builder()
                        .name(habit.getName())
                        .isCompleted(isCompleted)
                        .subscribersCount(subscribersCount)
                        .frequencyType(frequencyType)
                        .completionsInPeriod(completionsInPeriod)
                        .completionsPlannedInPeriod(completionsPlannedInPeriod)
                        .build());
            }
        }

        return currentUserHabitsAtDay;
    }

    public List<SubscribedHabitShortInfoResponse> getAllUserSubscribedHabitsAtDay(LocalDate date, String userId) {
        Long userIdLong = parseUserId(userId);

        List<Long> allUserSubscribedHabitsIds = subscriptionCacheRepository
                .findAllById_SubscriberId(userIdLong).stream()
                .map(subscriptionCache -> subscriptionCache.getId().getHabitId())
                .toList();

        List<Habit> allUserSubscribedHabits = habitRepository.findAllByIdIn(allUserSubscribedHabitsIds);

        List<SubscribedHabitShortInfoResponse> currentUserSubscribedHabitsAtDay = new ArrayList<>();

        for (Habit habit : allUserSubscribedHabits) {

            Long habitId = habit.getId();
            Long creatorId = habit.getUserId();

            if (internalHabitService.isCurrent(habitId, creatorId, date)) {

                String creatorLogin = subscriptionCacheRepository.findAllById_HabitId(habitId).get(0).getCreatorLogin();

                FrequencyType frequencyType = habit.getFrequencyType();

                int subscribersCount = subscriptionCacheRepository.countById_HabitId(habitId);

                boolean isCompleted = reportClient.isCompletedAtDay(habitId, date).getBody();

                Integer completionsInPeriod = null;

                if (frequencyType == FrequencyType.WEEKLY_X_TIMES) {
                    completionsInPeriod = reportClient.countCompletionsInPeriod(habitId, Period.WEEK, date).getBody();
                }

                if (frequencyType == FrequencyType.MONTHLY_X_TIMES) {
                    completionsInPeriod = reportClient.countCompletionsInPeriod(habitId, Period.MONTH, date).getBody();
                }

                Integer completionsPlannedInPeriod = null;

                if (frequencyType == FrequencyType.WEEKLY_X_TIMES) {
                    completionsPlannedInPeriod = habit.getTimesPerWeek();
                }

                if (frequencyType == FrequencyType.MONTHLY_X_TIMES) {
                    completionsPlannedInPeriod = habit.getTimesPerMonth();
                }

                currentUserSubscribedHabitsAtDay.add(SubscribedHabitShortInfoResponse.builder()
                        .creatorLogin(creatorLogin)
                        .name(habit.getName())
                        .isCompleted(isCompleted)
                        .subscribersCount(subscribersCount)
                        .frequencyType(frequencyType)
                        .completionsInPeriod(completionsInPeriod)
                        .completionsPlannedInPeriod(completionsPlannedInPeriod)
                        .build());
            }
        }

        return currentUserSubscribedHabitsAtDay;
    }

    private boolean isUserEitherHabitCreatorOrSubscriber(Long habitId, Long userId) {
        boolean isUserCreator = habitRepository.existsByIdAndUserId(habitId, userId);

        boolean isUserSubscriber = subscriptionCacheRepository.existsById(
                SubscriptionCacheId.builder()
                        .habitId(habitId)
                        .subscriberId(userId)
                        .build()
        );

        return isUserCreator || isUserSubscriber;
    }

}
