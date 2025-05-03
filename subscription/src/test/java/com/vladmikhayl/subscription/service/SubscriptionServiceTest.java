package com.vladmikhayl.subscription.service;

import com.vladmikhayl.commons.dto.AcceptedSubscriptionCreatedEvent;
import com.vladmikhayl.commons.dto.AcceptedSubscriptionDeletedEvent;
import com.vladmikhayl.subscription.dto.response.AcceptedSubscriptionForCreatorResponse;
import com.vladmikhayl.subscription.dto.response.AcceptedSubscriptionForSubscriberResponse;
import com.vladmikhayl.subscription.dto.response.UnprocessedRequestForCreatorResponse;
import com.vladmikhayl.subscription.dto.response.UnprocessedRequestForSubscriberResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Value("${internal.token}")
    private String testInternalToken;

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
                .hasMessage("У вас уже есть подписка/заявка на эту привычку");
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
                .hasMessage("Привычка не найдена");
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
                .hasMessageContaining("Нельзя подписаться на собственную привычку");
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

        when(authClient.getUserLogin(testInternalToken, 7L)).thenReturn(ResponseEntity.ok("user7"));

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
                .hasMessage("Заявка не найдена");

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
                .hasMessage("Заявка уже была принята");

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
                .hasMessageContaining("Текущий пользователь не является создателем привычки, на которую отправлена эта заявка");

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
                .hasMessage("Заявка не найдена");

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
                .hasMessageContaining("Текущий пользователь не является создателем привычки, на которую отправлена эта заявка");

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
                .hasMessageContaining("Заявка уже была принята");

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
                .hasMessage("Подписка/заявка не найдена");

        verify(subscriptionRepository, never()).delete(any());

        verify(subscriptionEventProducer, never()).sendAcceptedSubscriptionDeletedEvent(any());
    }

    @Test
    void testGetUserUnprocessedRequestsWithSomeUnprocessedRequests() {
        Long userId = 7L;
        String userIdStr = "7";

        List<Subscription> userSubscriptions = List.of(
                Subscription.builder()
                        .habitId(10L)
                        .subscriberId(userId)
                        .isAccepted(true)
                        .build(),
                Subscription.builder()
                        .habitId(11L)
                        .subscriberId(userId)
                        .isAccepted(false)
                        .build(),
                Subscription.builder()
                        .habitId(12L)
                        .subscriberId(userId)
                        .isAccepted(false)
                        .build()
        );

        when(subscriptionRepository.findAllBySubscriberId(userId)).thenReturn(userSubscriptions);

        when(habitCacheRepository.findByHabitId(11L)).thenReturn(Optional.of(
                HabitCache.builder()
                        .habitId(11L)
                        .habitName("Название 11")
                        .build()
        ));

        when(habitCacheRepository.findByHabitId(12L)).thenReturn(Optional.of(
                HabitCache.builder()
                        .habitId(12L)
                        .habitName("Название 12")
                        .build()
        ));

        List<UnprocessedRequestForSubscriberResponse> response = underTest.getUserUnprocessedRequests(userIdStr);

        assertThat(response).isEqualTo(
                List.of(
                        UnprocessedRequestForSubscriberResponse.builder()
                                .habitId(11L)
                                .habitName("Название 11")
                                .build(),
                        UnprocessedRequestForSubscriberResponse.builder()
                                .habitId(12L)
                                .habitName("Название 12")
                                .build()
                )
        );
    }

    @Test
    void testGetUserUnprocessedRequestsWithoutUnprocessedRequests() {
        Long userId = 7L;
        String userIdStr = "7";

        List<Subscription> userSubscriptions = List.of(
                Subscription.builder()
                        .habitId(10L)
                        .subscriberId(userId)
                        .isAccepted(true)
                        .build()
        );

        when(subscriptionRepository.findAllBySubscriberId(userId)).thenReturn(userSubscriptions);

        List<UnprocessedRequestForSubscriberResponse> response = underTest.getUserUnprocessedRequests(userIdStr);

        assertThat(response).isEqualTo(
                List.of()
        );
    }

    @Test
    void canGetHabitUnprocessedRequestsWithZeroRequests() {
        Long habitId = 15L;
        Long userId = 7L;
        String userIdStr = "7";

        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(userId)
                .build();

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.of(habitCache));

        when(subscriptionRepository.findAllByHabitId(habitId)).thenReturn(List.of());

        List<UnprocessedRequestForCreatorResponse> response = underTest.getHabitUnprocessedRequests(habitId, userIdStr);

        assertThat(response).isEqualTo(List.of());
    }

    @Test
    void canGetHabitUnprocessedRequestsWithZeroUnprocessedRequests() {
        Long habitId = 15L;
        Long userId = 7L;
        String userIdStr = "7";

        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(userId)
                .build();

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.of(habitCache));

        when(subscriptionRepository.findAllByHabitId(habitId)).thenReturn(List.of(
                Subscription.builder()
                        .habitId(habitId)
                        .subscriberId(10L)
                        .isAccepted(true)
                        .build()
        ));

        List<UnprocessedRequestForCreatorResponse> response = underTest.getHabitUnprocessedRequests(habitId, userIdStr);

        assertThat(response).isEqualTo(List.of());
    }

    @Test
    void canGetHabitUnprocessedRequestsWithSomeUnprocessedRequests() {
        Long habitId = 15L;
        Long userId = 7L;
        String userIdStr = "7";

        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(userId)
                .build();

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.of(habitCache));

        when(subscriptionRepository.findAllByHabitId(habitId)).thenReturn(List.of(
                Subscription.builder()
                        .id(50L)
                        .habitId(habitId)
                        .subscriberId(10L)
                        .isAccepted(true)
                        .build(),
                Subscription.builder()
                        .id(51L)
                        .habitId(habitId)
                        .subscriberId(11L)
                        .isAccepted(false)
                        .build(),
                Subscription.builder()
                        .id(52L)
                        .habitId(habitId)
                        .subscriberId(12L)
                        .isAccepted(false)
                        .build()
        ));

        when(authClient.getUserLogin(testInternalToken, 11L)).thenReturn(ResponseEntity.ok("user11"));
        when(authClient.getUserLogin(testInternalToken, 12L)).thenReturn(ResponseEntity.ok("user12"));

        List<UnprocessedRequestForCreatorResponse> response = underTest.getHabitUnprocessedRequests(habitId, userIdStr);

        assertThat(response).isEqualTo(List.of(
                UnprocessedRequestForCreatorResponse.builder()
                        .subscriptionId(51L)
                        .subscriberLogin("user11")
                        .build(),
                UnprocessedRequestForCreatorResponse.builder()
                        .subscriptionId(52L)
                        .subscriberLogin("user12")
                        .build()
        ));
    }

    @Test
    void failGetHabitUnprocessedRequestsWhenHabitNotFound() {
        Long habitId = 15L;
        String userIdStr = "7";

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getHabitUnprocessedRequests(habitId, userIdStr))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Привычка не найдена");
    }

    @Test
    void failGetHabitUnprocessedRequestsWhenHabitBelongsToAnotherUser() {
        Long habitId = 15L;
        String userIdStr = "7";

        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(8L)
                .build();

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.of(habitCache));

        assertThatThrownBy(() -> underTest.getHabitUnprocessedRequests(habitId, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .hasMessageContaining("Эта привычка не принадлежит текущему пользователю");
    }

    @Test
    void testGetUserAcceptedSubscriptionsWithoutAcceptedRequests() {
        Long userId = 7L;
        String userIdStr = "7";

        List<Subscription> userSubscriptions = List.of(
                Subscription.builder()
                        .habitId(10L)
                        .subscriberId(userId)
                        .isAccepted(false)
                        .build()
        );

        when(subscriptionRepository.findAllBySubscriberId(userId)).thenReturn(userSubscriptions);

        List<AcceptedSubscriptionForSubscriberResponse> response = underTest.getUserAcceptedSubscriptions(userIdStr);

        assertThat(response).isEqualTo(
                List.of()
        );
    }

    @Test
    void testGetUserAcceptedSubscriptionsWithSomeAcceptedRequests() {
        Long userId = 7L;
        String userIdStr = "7";

        List<Subscription> userSubscriptions = List.of(
                Subscription.builder()
                        .habitId(10L)
                        .subscriberId(userId)
                        .isAccepted(false)
                        .build(),
                Subscription.builder()
                        .habitId(11L)
                        .subscriberId(userId)
                        .isAccepted(true)
                        .build(),
                Subscription.builder()
                        .habitId(12L)
                        .subscriberId(userId)
                        .isAccepted(true)
                        .build()
        );

        when(subscriptionRepository.findAllBySubscriberId(userId)).thenReturn(userSubscriptions);

        when(habitCacheRepository.findByHabitId(11L)).thenReturn(Optional.of(
                HabitCache.builder()
                        .habitId(11L)
                        .habitName("Название 11")
                        .build()
        ));

        when(habitCacheRepository.findByHabitId(12L)).thenReturn(Optional.of(
                HabitCache.builder()
                        .habitId(12L)
                        .habitName("Название 12")
                        .build()
        ));

        List<AcceptedSubscriptionForSubscriberResponse> response = underTest.getUserAcceptedSubscriptions(userIdStr);

        assertThat(response).isEqualTo(List.of(
                AcceptedSubscriptionForSubscriberResponse.builder()
                        .habitId(11L)
                        .habitName("Название 11")
                        .build(),
                AcceptedSubscriptionForSubscriberResponse.builder()
                        .habitId(12L)
                        .habitName("Название 12")
                        .build()
        ));
    }

    @Test
    void canGetHabitAcceptedSubscriptionsWithZeroRequests() {
        Long habitId = 15L;
        Long userId = 7L;
        String userIdStr = "7";

        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(userId)
                .build();

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.of(habitCache));

        when(subscriptionRepository.findAllByHabitId(habitId)).thenReturn(List.of());

        List<AcceptedSubscriptionForCreatorResponse> response = underTest.getHabitAcceptedSubscriptions(habitId, userIdStr);

        assertThat(response).isEqualTo(List.of());
    }

    @Test
    void canGetHabitAcceptedSubscriptionsWithZeroAcceptedRequests() {
        Long habitId = 15L;
        Long userId = 7L;
        String userIdStr = "7";

        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(userId)
                .build();

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.of(habitCache));

        when(subscriptionRepository.findAllByHabitId(habitId)).thenReturn(List.of(
                Subscription.builder()
                        .habitId(habitId)
                        .subscriberId(10L)
                        .isAccepted(false)
                        .build()
        ));

        List<AcceptedSubscriptionForCreatorResponse> response = underTest.getHabitAcceptedSubscriptions(habitId, userIdStr);

        assertThat(response).isEqualTo(List.of());
    }

    @Test
    void canGetHabitAcceptedSubscriptionsWithSomeAcceptedRequests() {
        Long habitId = 15L;
        Long userId = 7L;
        String userIdStr = "7";

        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(userId)
                .build();

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.of(habitCache));

        when(subscriptionRepository.findAllByHabitId(habitId)).thenReturn(List.of(
                Subscription.builder()
                        .habitId(habitId)
                        .subscriberId(10L)
                        .isAccepted(false)
                        .build(),
                Subscription.builder()
                        .habitId(habitId)
                        .subscriberId(11L)
                        .isAccepted(true)
                        .build(),
                Subscription.builder()
                        .habitId(habitId)
                        .subscriberId(12L)
                        .isAccepted(true)
                        .build()
        ));

        when(authClient.getUserLogin(testInternalToken, 11L)).thenReturn(ResponseEntity.ok("user11"));
        when(authClient.getUserLogin(testInternalToken, 12L)).thenReturn(ResponseEntity.ok("user12"));

        List<AcceptedSubscriptionForCreatorResponse> response = underTest.getHabitAcceptedSubscriptions(habitId, userIdStr);

        assertThat(response).isEqualTo(List.of(
                AcceptedSubscriptionForCreatorResponse.builder()
                        .subscriberLogin("user11")
                        .build(),
                AcceptedSubscriptionForCreatorResponse.builder()
                        .subscriberLogin("user12")
                        .build()
        ));
    }

    @Test
    void failGetHabitAcceptedSubscriptionsWhenHabitNotFound() {
        Long habitId = 15L;
        String userIdStr = "7";

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getHabitAcceptedSubscriptions(habitId, userIdStr))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Привычка не найдена");
    }

    @Test
    void failGetHabitAcceptedSubscriptionsWhenHabitBelongsToAnotherUser() {
        Long habitId = 15L;
        String userIdStr = "7";

        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(8L)
                .build();

        when(habitCacheRepository.findByHabitId(habitId)).thenReturn(Optional.of(habitCache));

        assertThatThrownBy(() -> underTest.getHabitAcceptedSubscriptions(habitId, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .hasMessageContaining("Эта привычка не принадлежит текущему пользователю");
    }

}