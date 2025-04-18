package com.vladmikhayl.subscription.repository;

import com.vladmikhayl.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByHabitIdAndSubscriberId(Long habitId, Long subscriberId);

    void deleteByHabitId(Long habitId);

    Optional<Subscription> findByHabitIdAndSubscriberId(Long habitId, Long subscriberId);

    List<Subscription> findAllBySubscriberId(Long subscriberId);

}
