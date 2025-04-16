package com.vladmikhayl.subscription.service.kafka;

import com.vladmikhayl.habit.dto.event.HabitCreatedEvent;
import com.vladmikhayl.habit.dto.event.HabitDeletedEvent;
import com.vladmikhayl.subscription.entity.HabitCache;
import com.vladmikhayl.subscription.repository.HabitCacheRepository;
import com.vladmikhayl.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionListener {

    private final HabitCacheRepository habitCacheRepository;

    private final SubscriptionRepository subscriptionRepository;

    @KafkaListener(topics = "habit-created", groupId = "subscription-group")
    public void listen(HabitCreatedEvent event) {
        System.out.println("Получено событие: создана привычка " + event.habitId());

        habitCacheRepository.save(
                HabitCache.builder()
                        .creatorId(event.userId())
                        .habitId(event.habitId())
                        .build()
        );

        System.out.println("В таблицу habits_cache добавлена привычка " + event.habitId());
    }

    @Transactional
    @KafkaListener(topics = "habit-deleted", groupId = "subscription-group")
    public void listen(HabitDeletedEvent event) {
        Long habitId = event.habitId();

        System.out.println("Получено событие: удалена привычка " + habitId);

        habitCacheRepository.deleteByHabitId(habitId);

        System.out.println("Из таблицы habits_cache удалена привычка " + habitId);

        subscriptionRepository.deleteByHabitId(habitId);

        System.out.println("Удалены все подписки/заявки на привычку " + habitId + " (если они там существовали)");
    }

}
