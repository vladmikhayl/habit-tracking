package com.vladmikhayl.habit.entity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitWithoutAutoCreationTimeRepository extends JpaRepository<HabitWithoutAutoCreationTime, Long> {
}
