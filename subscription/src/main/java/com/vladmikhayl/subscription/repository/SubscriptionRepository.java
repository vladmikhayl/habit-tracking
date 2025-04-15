package com.vladmikhayl.subscription.repository;

import com.vladmikhayl.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByHabitIdAndSubscriberId(Long habitId, Long subscriberId);

}
