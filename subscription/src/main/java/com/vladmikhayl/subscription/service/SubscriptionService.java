package com.vladmikhayl.subscription.service;

import com.vladmikhayl.commons.dto.AcceptedSubscriptionCreatedEvent;
import com.vladmikhayl.commons.dto.AcceptedSubscriptionDeletedEvent;
import com.vladmikhayl.subscription.dto.response.AcceptedSubscriptionForCreatorResponse;
import com.vladmikhayl.subscription.dto.response.AcceptedSubscriptionForSubscriberResponse;
import com.vladmikhayl.subscription.dto.response.UnprocessedRequestForCreatorResponse;
import com.vladmikhayl.subscription.dto.response.UnprocessedRequestForSubscriberResponse;
import com.vladmikhayl.subscription.entity.Subscription;
import com.vladmikhayl.subscription.repository.HabitCacheRepository;
import com.vladmikhayl.subscription.repository.SubscriptionRepository;
import com.vladmikhayl.subscription.service.feign.AuthClient;
import com.vladmikhayl.subscription.service.kafka.SubscriptionEventProducer;
import feign.FeignException;
import feign.RetryableException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    @Value("${internal.token}")
    private String internalToken;

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

        String habitCreatorLogin = getLoginOrThrow(habitCreatorId);

        // Отправка события о появлении принятой подписки всем, кто подписан на accepted-subscription-created
        AcceptedSubscriptionCreatedEvent event = AcceptedSubscriptionCreatedEvent.builder()
                .habitId(subscription.getHabitId())
                .subscriberId(subscription.getSubscriberId())
                .habitCreatorLogin(habitCreatorLogin)
                .build();
        subscriptionEventProducer.sendAcceptedSubscriptionCreatedEvent(event);
    }

    public void denySubscriptionRequest(Long subscriptionId, String userId) {
        Long userIdLong = parseUserId(userId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription request not found"));

        Long habitCreatorId = habitCacheRepository.findByHabitId(subscription.getHabitId())
                .orElseThrow(() -> new EntityNotFoundException("Habit not found"))
                .getCreatorId();

        if (!userIdLong.equals(habitCreatorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The user is not the creator of the habit that the subscription request is for");
        }

        if (subscription.isAccepted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription request is already accepted");
        }

        subscriptionRepository.delete(subscription);
    }

    public void unsubscribe(Long habitId, String userId) {
        Long userIdLong = parseUserId(userId);

        Subscription subscription = subscriptionRepository.findByHabitIdAndSubscriberId(habitId, userIdLong)
                .orElseThrow(() -> new EntityNotFoundException("Subscription (or subscription request) not found"));

        subscriptionRepository.delete(subscription);

        if (subscription.isAccepted()) {
            // Отправка события об удалении принятой подписки всем, кто подписан на accepted-subscription-deleted
            AcceptedSubscriptionDeletedEvent event = AcceptedSubscriptionDeletedEvent.builder()
                    .habitId(subscription.getHabitId())
                    .subscriberId(subscription.getSubscriberId())
                    .build();
            subscriptionEventProducer.sendAcceptedSubscriptionDeletedEvent(event);
        }
    }

    public List<UnprocessedRequestForSubscriberResponse> getUserUnprocessedRequests(String userId) {
        Long userIdLong = parseUserId(userId);

        return subscriptionRepository.findAllBySubscriberId(userIdLong).stream()
                .filter(subscription -> !subscription.isAccepted())
                .map(subscription ->
                        UnprocessedRequestForSubscriberResponse.builder()
                                .habitId(subscription.getHabitId())
                                .habitName(getHabitName(subscription.getHabitId()))
                                .build()
                )
                .toList();
    }

    public List<UnprocessedRequestForCreatorResponse> getHabitUnprocessedRequests(Long habitId, String userId) {
        Long userIdLong = parseUserId(userId);

        Long habitCreatorId = habitCacheRepository.findByHabitId(habitId)
                .orElseThrow(() -> new EntityNotFoundException("Habit not found"))
                .getCreatorId();

        if (!Objects.equals(habitCreatorId, userIdLong)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The habit doesn't belong to that user");
        }

        return subscriptionRepository.findAllByHabitId(habitId).stream()
                .filter(subscription -> !subscription.isAccepted())
                .map(subscription ->
                        UnprocessedRequestForCreatorResponse.builder()
                                .subscriptionId(subscription.getId())
                                .subscriberLogin(
                                        getLoginOrThrow(subscription.getSubscriberId())
                                )
                                .build()
                )
                .toList();
    }

    public List<AcceptedSubscriptionForSubscriberResponse> getUserAcceptedSubscriptions(String userId) {
        Long userIdLong = parseUserId(userId);

        return subscriptionRepository.findAllBySubscriberId(userIdLong).stream()
                .filter(Subscription::isAccepted)
                .map(subscription ->
                        AcceptedSubscriptionForSubscriberResponse.builder()
                                .habitId(subscription.getHabitId())
                                .habitName(getHabitName(subscription.getHabitId()))
                                .build()
                )
                .toList();
    }

    public List<AcceptedSubscriptionForCreatorResponse> getHabitAcceptedSubscriptions(Long habitId, String userId) {
        Long userIdLong = parseUserId(userId);

        Long habitCreatorId = habitCacheRepository.findByHabitId(habitId)
                .orElseThrow(() -> new EntityNotFoundException("Habit not found"))
                .getCreatorId();

        if (!Objects.equals(habitCreatorId, userIdLong)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The habit doesn't belong to that user");
        }

        return subscriptionRepository.findAllByHabitId(habitId).stream()
                .filter(Subscription::isAccepted)
                .map(subscription ->
                        AcceptedSubscriptionForCreatorResponse.builder()
                                .subscriberLogin(
                                        getLoginOrThrow(subscription.getSubscriberId())
                                )
                                .build()
                )
                .toList();
    }

    private String getHabitName(Long habitId) {
        return habitCacheRepository.findByHabitId(habitId)
                .orElseThrow(() -> new EntityNotFoundException("Habit not found"))
                .getHabitName();
    }

    private String getLoginOrThrow(Long userId) {
        try {
            return authClient.getUserLogin(internalToken, userId).getBody();
        } catch (FeignException.ServiceUnavailable e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Auth service is unavailable");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Auth service returned an error");
        }
    }

}
