package com.vladmikhayl.habit.service;

import com.vladmikhayl.habit.entity.Habit;
import com.vladmikhayl.habit.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepository habitRepository;

    public List<Habit> getAllUserHabits(Long userId) {
        return habitRepository.findAllByUserId(userId);
    }

}
