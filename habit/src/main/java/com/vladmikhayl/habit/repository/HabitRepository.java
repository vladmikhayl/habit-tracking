package com.vladmikhayl.habit.repository;

import com.vladmikhayl.habit.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findAllByUserId(Long userId);

    int countByUserIdAndName(Long userId, String name);

}
