package com.vladmikhayl.habit.service;

import com.vladmikhayl.habit.dto.request.HabitCreationRequest;
import com.vladmikhayl.habit.dto.request.HabitEditingRequest;
import com.vladmikhayl.habit.dto.event.HabitCreatedEvent;
import com.vladmikhayl.habit.dto.event.HabitDeletedEvent;
import com.vladmikhayl.habit.entity.FrequencyType;
import com.vladmikhayl.habit.entity.Habit;
import com.vladmikhayl.habit.repository.HabitRepository;
import com.vladmikhayl.habit.service.kafka.HabitEventProducer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepository habitRepository;

    private final HabitEventProducer habitEventProducer;

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

}
