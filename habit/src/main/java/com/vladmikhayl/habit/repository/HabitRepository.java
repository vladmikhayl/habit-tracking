package com.vladmikhayl.habit.repository;

import com.vladmikhayl.habit.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    Optional<Habit> findByIdAndUserId(Long id, Long userId);

    Optional<Habit> findByName(String name);

    boolean existsByUserIdAndName(Long userId, String name);

    boolean existsByIdAndUserId(Long id, Long userId);

    List<Habit> findAllByUserId(Long userId);

}
