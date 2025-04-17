package com.vladmikhayl.subscription.service;

import com.vladmikhayl.subscription.dto.event.AcceptedSubscriptionCreatedEvent;
import com.vladmikhayl.subscription.entity.Subscription;
import com.vladmikhayl.subscription.repository.HabitCacheRepository;
import com.vladmikhayl.subscription.repository.SubscriptionRepository;
import com.vladmikhayl.subscription.service.feign.AuthClient;
import com.vladmikhayl.subscription.service.kafka.SubscriptionEventProducer;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    private final HabitCacheRepository habitCacheRepository;

    private final SubscriptionEventProducer subscriptionEventProducer;

    private final AuthClient authClient;

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid user ID format");
        }
    }

    public void sendSubscriptionRequest(Long habitId, String userId) {
        Long userIdLong = parseUserId(userId);

        boolean doesThatSubscriptionAlreadyExists = subscriptionRepository.existsByHabitIdAndSubscriberId(habitId, userIdLong);

        if (doesThatSubscriptionAlreadyExists) {
            throw new DataIntegrityViolationException(
                    "This user is already subscribed to this habit or has an unprocessed request for it"
            );
        }

        Long habitCreatorId = habitCacheRepository.findByHabitId(habitId)
                .orElseThrow(() -> new EntityNotFoundException("Habit not found"))
                .getCreatorId();

        if (Objects.equals(habitCreatorId, userIdLong)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "It is impossible to subscribe to your own habit");
        }

        subscriptionRepository.save(
                Subscription.builder()
                        .habitId(habitId)
                        .subscriberId(userIdLong)
                        .isAccepted(false)
                        .build()
        );
    }

    @Transactional
    public void acceptSubscriptionRequest(Long subscriptionId, String userId) {
        Long userIdLong = parseUserId(userId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription request not found"));

        if (subscription.isAccepted()) {
            throw new DataIntegrityViolationException("Subscription request has already been accepted");
        }

        Long habitCreatorId = habitCacheRepository.findByHabitId(subscription.getHabitId())
                .orElseThrow(() -> new EntityNotFoundException("Habit not found"))
                .getCreatorId();

        if (!userIdLong.equals(habitCreatorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The user is not the creator of the habit that the subscription request is for");
        }

        subscription.setAccepted(true);

        String habitCreatorLogin = authClient.getUserLogin(habitCreatorId).getBody();

        // Отправка события о появлении принятой подписки всем, кто подписан на accepted-subscription-created
        AcceptedSubscriptionCreatedEvent event = AcceptedSubscriptionCreatedEvent.builder()
                .habitId(subscription.getHabitId())
                .subscriberId(userIdLong)
                .habitCreatorLogin(habitCreatorLogin)
                .build();
        subscriptionEventProducer.sendAcceptedSubscriptionCreatedEvent(event);
    }

}
