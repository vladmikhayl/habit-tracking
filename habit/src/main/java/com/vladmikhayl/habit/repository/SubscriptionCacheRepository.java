package com.vladmikhayl.habit.repository;

import com.vladmikhayl.habit.entity.SubscriptionCache;
import com.vladmikhayl.habit.entity.SubscriptionCacheId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionCacheRepository extends JpaRepository<SubscriptionCache, SubscriptionCacheId> {

    int countById_HabitId(Long habitId);

    List<SubscriptionCache> findAllById_SubscriberId(Long subscriberId);

    List<SubscriptionCache> findAllById_HabitId(Long habitId);

}
