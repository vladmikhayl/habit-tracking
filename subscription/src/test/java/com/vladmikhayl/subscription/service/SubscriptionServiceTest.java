package com.vladmikhayl.subscription.service;

import com.vladmikhayl.commons.dto.AcceptedSubscriptionCreatedEvent;
import com.vladmikhayl.commons.dto.AcceptedSubscriptionDeletedEvent;
import com.vladmikhayl.subscription.entity.HabitCache;
import com.vladmikhayl.subscription.entity.Subscription;
import com.vladmikhayl.subscription.repository.HabitCacheRepository;
import com.vladmikhayl.subscription.repository.SubscriptionRepository;
import com.vladmikhayl.subscription.service.feign.AuthClient;
import com.vladmikhayl.subscription.service.kafka.SubscriptionEventProducer;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private HabitCacheRepository habitCacheRepository;

    @Mock
    private AuthClient authClient;

    @Mock
    private SubscriptionEventProducer subscriptionEventProducer;

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

    @Test
    void canAcceptSubscriptionRequestWhenThatRequestIsForHabitOfCurrentUser() {
        Long subscriptionId = 46L;
        Long habitId = 15L;
        String userIdStr = "7";

        Subscription subscription = Subscription.builder()
                .id(subscriptionId)
                .habitId(habitId)
                .subscriberId(50L)
                .isAccepted(false)
                .build();

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(7L)
                .build();

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.of(habitCache));

        when(authClient.getUserLogin(7L)).thenReturn(ResponseEntity.ok("user7"));

        underTest.acceptSubscriptionRequest(subscriptionId, userIdStr);

        assertThat(subscription.isAccepted()).isTrue();

        ArgumentCaptor<AcceptedSubscriptionCreatedEvent> eventArgumentCaptor =
                ArgumentCaptor.forClass(AcceptedSubscriptionCreatedEvent.class);

        verify(subscriptionEventProducer).sendAcceptedSubscriptionCreatedEvent(eventArgumentCaptor.capture());

        AcceptedSubscriptionCreatedEvent event = eventArgumentCaptor.getValue();

        assertThat(event.habitId()).isEqualTo(habitId);
        assertThat(event.subscriberId()).isEqualTo(50L);
        assertThat(event.habitCreatorLogin()).isEqualTo("user7");
    }

    @Test
    void failAcceptSubscriptionRequestWhenThatRequestDoesNotExist() {
        Long subscriptionId = 46L;
        String userIdStr = "7";

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.acceptSubscriptionRequest(subscriptionId, userIdStr))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Subscription request not found");

        verify(subscriptionEventProducer, never()).sendAcceptedSubscriptionCreatedEvent(any());
    }

    @Test
    void failAcceptSubscriptionRequestWhenThatRequestIsAlreadyAccepted() {
        Long subscriptionId = 46L;
        String userIdStr = "7";

        Subscription subscription = Subscription.builder()
                .id(subscriptionId)
                .isAccepted(true)
                .build();

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> underTest.acceptSubscriptionRequest(subscriptionId, userIdStr))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("Subscription request has already been accepted");

        verify(subscriptionEventProducer, never()).sendAcceptedSubscriptionCreatedEvent(any());
    }

    @Test
    void failAcceptSubscriptionRequestWhenThatRequestIsNotForHabitOfCurrentUser() {
        Long subscriptionId = 46L;
        Long habitId = 15L;
        String userIdStr = "7";

        Subscription subscription = Subscription.builder()
                .id(subscriptionId)
                .habitId(habitId)
                .subscriberId(50L)
                .isAccepted(false)
                .build();

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(8L)
                .build();

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.of(habitCache));

        assertThatThrownBy(() -> underTest.acceptSubscriptionRequest(subscriptionId, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .hasMessageContaining("The user is not the creator of the habit that the subscription request is for");

        verify(subscriptionEventProducer, never()).sendAcceptedSubscriptionCreatedEvent(any());
    }

    @Test
    void canDenySubscriptionRequestWhenThatRequestIsForHabitOfCurrentUser() {
        Long subscriptionId = 46L;
        Long habitId = 15L;
        String userIdStr = "7";

        Subscription subscription = Subscription.builder()
                .id(subscriptionId)
                .habitId(habitId)
                .subscriberId(50L)
                .isAccepted(false)
                .build();

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(7L)
                .build();

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.of(habitCache));

        underTest.denySubscriptionRequest(subscriptionId, userIdStr);

        ArgumentCaptor<Subscription> subscriptionArgumentCaptor = ArgumentCaptor.forClass(Subscription.class);

        verify(subscriptionRepository).delete(subscriptionArgumentCaptor.capture());

        Subscription deletedSubscription = subscriptionArgumentCaptor.getValue();

        assertThat(deletedSubscription.getId()).isEqualTo(subscriptionId);
    }

    @Test
    void failDenySubscriptionRequestWhenThatRequestDoesNotExist() {
        Long subscriptionId = 46L;
        String userIdStr = "7";

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.denySubscriptionRequest(subscriptionId, userIdStr))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Subscription request not found");

        verify(subscriptionRepository, never()).delete(any());
    }

    @Test
    void failDenySubscriptionRequestWhenThatRequestIsNotForHabitOfCurrentUser() {
        Long subscriptionId = 46L;
        Long habitId = 15L;
        String userIdStr = "7";

        Subscription subscription = Subscription.builder()
                .id(subscriptionId)
                .habitId(habitId)
                .subscriberId(50L)
                .isAccepted(false)
                .build();

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(8L)
                .build();

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.of(habitCache));

        assertThatThrownBy(() -> underTest.denySubscriptionRequest(subscriptionId, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .hasMessageContaining("The user is not the creator of the habit that the subscription request is for");

        verify(subscriptionRepository, never()).delete(any());
    }

    @Test
    void failDenySubscriptionRequestWhenThatRequestIsAlreadyAccepted() {
        Long subscriptionId = 46L;
        Long habitId = 15L;
        String userIdStr = "7";

        Subscription subscription = Subscription.builder()
                .id(subscriptionId)
                .habitId(habitId)
                .subscriberId(50L)
                .isAccepted(true)
                .build();

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(7L)
                .build();

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.of(habitCache));

        assertThatThrownBy(() -> underTest.denySubscriptionRequest(subscriptionId, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                })
                .hasMessageContaining("Subscription request is already accepted");

        verify(subscriptionRepository, never()).delete(any());
    }

    @Test
    void canUnsubscribeWithNotAcceptedSubscription() {
        Long habitId = 15L;
        Long userId = 7L;
        String userIdStr = "7";

        Subscription subscription = Subscription.builder()
                .habitId(habitId)
                .subscriberId(userId)
                .isAccepted(false)
                .build();

        when(subscriptionRepository.findByHabitIdAndSubscriberId(habitId, userId)).thenReturn(Optional.of(subscription));

        underTest.unsubscribe(habitId, userIdStr);

        ArgumentCaptor<Subscription> subscriptionArgumentCaptor = ArgumentCaptor.forClass(Subscription.class);

        verify(subscriptionRepository).delete(subscriptionArgumentCaptor.capture());

        Subscription deletedSubscription = subscriptionArgumentCaptor.getValue();

        assertThat(deletedSubscription.getHabitId()).isEqualTo(habitId);
        assertThat(deletedSubscription.getSubscriberId()).isEqualTo(userId);

        verify(subscriptionEventProducer, never()).sendAcceptedSubscriptionDeletedEvent(any());
    }

    @Test
    void canUnsubscribeWithAcceptedSubscription() {
        Long habitId = 15L;
        Long userId = 7L;
        String userIdStr = "7";

        Subscription subscription = Subscription.builder()
                .habitId(habitId)
                .subscriberId(userId)
                .isAccepted(true)
                .build();

        when(subscriptionRepository.findByHabitIdAndSubscriberId(habitId, userId)).thenReturn(Optional.of(subscription));

        underTest.unsubscribe(habitId, userIdStr);

        ArgumentCaptor<Subscription> subscriptionArgumentCaptor = ArgumentCaptor.forClass(Subscription.class);

        verify(subscriptionRepository).delete(subscriptionArgumentCaptor.capture());

        Subscription deletedSubscription = subscriptionArgumentCaptor.getValue();

        assertThat(deletedSubscription.getHabitId()).isEqualTo(habitId);
        assertThat(deletedSubscription.getSubscriberId()).isEqualTo(userId);

        ArgumentCaptor<AcceptedSubscriptionDeletedEvent> eventArgumentCaptor =
                ArgumentCaptor.forClass(AcceptedSubscriptionDeletedEvent.class);

        verify(subscriptionEventProducer).sendAcceptedSubscriptionDeletedEvent(eventArgumentCaptor.capture());

        AcceptedSubscriptionDeletedEvent event = eventArgumentCaptor.getValue();

        assertThat(event.habitId()).isEqualTo(habitId);
        assertThat(event.subscriberId()).isEqualTo(userId);
    }

    @Test
    void failUnsubscribeWhenUserDoesNotHaveThatSubscription() {
        Long habitId = 15L;
        Long userId = 7L;
        String userIdStr = "7";

        when(subscriptionRepository.findByHabitIdAndSubscriberId(habitId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.unsubscribe(habitId, userIdStr))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Subscription (or subscription request) not found");

        verify(subscriptionRepository, never()).delete(any());

        verify(subscriptionEventProducer, never()).sendAcceptedSubscriptionDeletedEvent(any());
    }

}