package com.vladmikhayl.habit.service;

import com.vladmikhayl.habit.dto.HabitCreationRequest;
import com.vladmikhayl.habit.dto.HabitShortInfoResponse;
import com.vladmikhayl.habit.dto.HabitsListResponse;
import com.vladmikhayl.habit.entity.Habit;
import com.vladmikhayl.habit.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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
            HabitCreationRequest habitCreationRequest,
            String userId
    ) {
        Long userIdLong = parseUserId(userId);

        if (habitRepository.countByUserIdAndName(userIdLong, habitCreationRequest.getName()) > 0) {
            throw new DataIntegrityViolationException("This user already has a habit with that name");
        }

        Habit habit = Habit.builder()
                .userId(userIdLong)
                .name(habitCreationRequest.getName())
                .description(habitCreationRequest.getDescription())
                .frequency(habitCreationRequest.getFrequency())
                .photoAllowed(habitCreationRequest.isPhotoAllowed())
                .durationDays(habitCreationRequest.getDurationDays())
                .build();

        habitRepository.save(habit);
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
