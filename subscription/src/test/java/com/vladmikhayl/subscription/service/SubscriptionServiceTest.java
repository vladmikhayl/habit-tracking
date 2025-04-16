package com.vladmikhayl.subscription.service;

import com.vladmikhayl.subscription.entity.HabitCache;
import com.vladmikhayl.subscription.entity.Subscription;
import com.vladmikhayl.subscription.repository.HabitCacheRepository;
import com.vladmikhayl.subscription.repository.SubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private HabitCacheRepository habitCacheRepository;

    @InjectMocks
    private SubscriptionService underTest;

    @Test
    void canSendSubscriptionRequestWhenThatHabitBelongsToAnotherUser() {
        Long habitId = 15L;
        Long subscriberId = 7L;
        String subscriberIdStr = "7";

        when(subscriptionRepository.existsByHabitIdAndSubscriberId(habitId, subscriberId)).thenReturn(false);

        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(10L)
                .build();

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.of(habitCache));

        underTest.sendSubscriptionRequest(habitId, subscriberIdStr);

        ArgumentCaptor<Subscription> subscriptionArgumentCaptor = ArgumentCaptor.forClass(Subscription.class);

        verify(subscriptionRepository).save(subscriptionArgumentCaptor.capture());

        Subscription capturedSubscription = subscriptionArgumentCaptor.getValue();

        assertThat(capturedSubscription.getHabitId()).isEqualTo(15L);
        assertThat(capturedSubscription.getSubscriberId()).isEqualTo(7L);
        assertThat(capturedSubscription.isAccepted()).isEqualTo(false);
    }

    @Test
    void failSendSubscriptionRequestWhenThatSubscriptionAlreadyExists() {
        Long habitId = 15L;
        Long subscriberId = 7L;
        String subscriberIdStr = "7";

        when(subscriptionRepository.existsByHabitIdAndSubscriberId(habitId, subscriberId)).thenReturn(true);

        assertThatThrownBy(() -> underTest.sendSubscriptionRequest(habitId, subscriberIdStr))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("This user is already subscribed to this habit or has an unprocessed request for it");
    }

    @Test
    void failSendSubscriptionRequestWhenThatHabitDoesNotExist() {
        Long habitId = 15L;
        Long subscriberId = 7L;
        String subscriberIdStr = "7";

        when(subscriptionRepository.existsByHabitIdAndSubscriberId(habitId, subscriberId)).thenReturn(false);

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.sendSubscriptionRequest(habitId, subscriberIdStr))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Habit not found");
    }

    @Test
    void failSendSubscriptionRequestWhenThatHabitBelongsToSubscriber() {
        Long habitId = 15L;
        Long subscriberId = 7L;
        String subscriberIdStr = "7";

        when(subscriptionRepository.existsByHabitIdAndSubscriberId(habitId, subscriberId)).thenReturn(false);

        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(subscriberId)
                .build();

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.of(habitCache));

        assertThatThrownBy(() -> underTest.sendSubscriptionRequest(habitId, subscriberIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                })
                .hasMessageContaining("It is impossible to subscribe to your own habit");
    }

}