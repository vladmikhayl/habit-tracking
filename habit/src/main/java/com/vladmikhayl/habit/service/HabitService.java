package com.vladmikhayl.habit.service;

import com.vladmikhayl.habit.dto.HabitCreationRequest;
import com.vladmikhayl.habit.dto.HabitEditingRequest;
import com.vladmikhayl.habit.dto.HabitShortInfoResponse;
import com.vladmikhayl.habit.dto.HabitsListResponse;
import com.vladmikhayl.habit.entity.FrequencyType;
import com.vladmikhayl.habit.entity.Habit;
import com.vladmikhayl.habit.repository.HabitRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepository habitRepository;

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

        habitRepository.save(habit);
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

    public HabitsListResponse getAllUserHabits(String userId) {
        Long userIdLong = parseUserId(userId);

        List<HabitShortInfoResponse> habitShortInfoResponseList = new ArrayList<>();

        habitRepository.findAllByUserId(userIdLong)
                .forEach(habit -> habitShortInfoResponseList.add(
                        HabitShortInfoResponse.builder()
                                .name(habit.getName())
                                .subsAmount(0) // TODO: вставалять реально кол-во подписчиков
                                .isCompletedNow(false) // TODO: вставлять реально выполнена ли сегодня
                                .build()
                ));

        return HabitsListResponse.builder()
                .habits(habitShortInfoResponseList)
                .build();
    }

}
