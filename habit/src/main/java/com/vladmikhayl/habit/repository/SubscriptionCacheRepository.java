package com.vladmikhayl.habit.repository;

import com.vladmikhayl.habit.entity.SubscriptionCache;
import com.vladmikhayl.habit.entity.SubscriptionCacheId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionCacheRepository extends JpaRepository<SubscriptionCache, SubscriptionCacheId> {
}
