package com.vladmikhayl.habit.service.kafka;

import com.vladmikhayl.commons.dto.AcceptedSubscriptionCreatedEvent;
import com.vladmikhayl.habit.entity.SubscriptionCache;
import com.vladmikhayl.habit.entity.SubscriptionCacheId;
import com.vladmikhayl.habit.repository.SubscriptionCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HabitListener {

    private final SubscriptionCacheRepository subscriptionCacheRepository;

    @KafkaListener(topics = "accepted-subscription-created", groupId = "habit-group")
    public void listen(AcceptedSubscriptionCreatedEvent event) {
        System.out.println(
                "Получено событие: появилась принятая подписка на привычку " + event.habitId() + " от юзера " + event.subscriberId()
        );

        subscriptionCacheRepository.save(
                SubscriptionCache.builder()
                        .id(
                                SubscriptionCacheId.builder()
                                        .habitId(event.habitId())
                                        .subscriberId(event.subscriberId())
                                        .build()
                        )
                        .creatorLogin(event.habitCreatorLogin())
                        .build()
        );

        System.out.println(
                "В таблицу subscriptions_cache добавлена подписка на привычку " + event.habitId() + " от юзера " + event.subscriberId()
        );
    }

}
