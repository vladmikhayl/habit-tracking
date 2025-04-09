package com.vladmikhayl.habit.service;

import com.vladmikhayl.habit.entity.FrequencyType;
import com.vladmikhayl.habit.entity.Habit;
import com.vladmikhayl.habit.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InternalHabitService {

    private final HabitRepository habitRepository;

    public boolean isCurrent(Long habitId, Long userId, LocalDate date) {
        Optional<Habit> habit = habitRepository.findByIdAndUserId(habitId, userId);

        if (habit.isEmpty()) {
            return false;
        }

        LocalDate createdAt = LocalDate.from(habit.get().getCreatedAt());

        if (createdAt.isAfter(date)) {
            return false;
        }

        Integer durationDays = habit.get().getDurationDays();

        if (durationDays != null && createdAt.plusDays(durationDays - 1).isBefore(date)) {
            return false;
        }

        FrequencyType frequencyType = habit.get().getFrequencyType();

        if (frequencyType == FrequencyType.WEEKLY_X_TIMES || frequencyType == FrequencyType.MONTHLY_X_TIMES) {
            return true;
        } else {
            Set<DayOfWeek> daysOfWeek = habit.get().getDaysOfWeek();
            return daysOfWeek.contains(date.getDayOfWeek());
        }
    }

}
