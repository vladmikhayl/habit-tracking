package com.vladmikhayl.subscription.repository;

import com.vladmikhayl.subscription.entity.HabitCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HabitCacheRepository extends JpaRepository<HabitCache, Long> {

    Optional<HabitCache> findByHabitId(Long habitId);

}
