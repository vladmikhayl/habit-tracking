package com.vladmikhayl.subscription.service.kafka;

import com.vladmikhayl.commons.dto.HabitCreatedEvent;
import com.vladmikhayl.commons.dto.HabitDeletedEvent;
import com.vladmikhayl.subscription.entity.HabitCache;
import com.vladmikhayl.subscription.repository.HabitCacheRepository;
import com.vladmikhayl.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionListener {

    private final HabitCacheRepository habitCacheRepository;

    private final SubscriptionRepository subscriptionRepository;

    @KafkaListener(topics = "habit-created", groupId = "subscription-group")
    public void listen(HabitCreatedEvent event) {
        log.info("Получено событие: создана привычка {}", event.habitId());

        habitCacheRepository.save(
                HabitCache.builder()
                        .creatorId(event.userId())
                        .habitId(event.habitId())
                        .habitName(event.habitName())
                        .build()
        );

        log.info("В таблицу habits_cache добавлена привычка {}", event.habitId());
    }

    @Transactional
    @KafkaListener(topics = "habit-deleted", groupId = "subscription-group")
    public void listen(HabitDeletedEvent event) {
        Long habitId = event.habitId();

        log.info("Получено событие: удалена привычка {}", habitId);

        habitCacheRepository.deleteByHabitId(habitId);

        log.info("Из таблицы habits_cache удалена привычка {}", habitId);

        subscriptionRepository.deleteByHabitId(habitId);

        log.info("Удалены все подписки/заявки на привычку {} (если они существовали)", habitId);
    }

}
