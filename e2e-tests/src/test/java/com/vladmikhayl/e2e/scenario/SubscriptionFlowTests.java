package com.vladmikhayl.e2e.scenario;

import com.vladmikhayl.e2e.BaseE2ETest;
import com.vladmikhayl.e2e.dto.habit.*;
import com.vladmikhayl.e2e.dto.subscription.AcceptedSubscriptionForCreatorResponse;
import com.vladmikhayl.e2e.dto.subscription.AcceptedSubscriptionForSubscriberResponse;
import com.vladmikhayl.e2e.dto.subscription.UnprocessedRequestForCreatorResponse;
import com.vladmikhayl.e2e.dto.subscription.UnprocessedRequestForSubscriberResponse;
import com.vladmikhayl.e2e.entity.FrequencyType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class SubscriptionFlowTests extends BaseE2ETest {

    @Test
    void testDenyingSubscriptionRequest() throws InterruptedException {
        // Создается юзер 1 с рандомным логином
        String userLogin1 = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin1, "12345");
        String token1 = authHelper.login(userLogin1, "12345").getToken();

        // Создается юзер 2 с рандомным логином
        String userLogin2 = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin2, "12345");
        String token2 = authHelper.login(userLogin2, "12345").getToken();

        // Юзер 1 создал привычку с WEEKLY_X_TIMES
        habitHelper.createHabit(
                token1,
                new HabitCreationRequest(
                        "Привычка 1",
                        null,
                        null,
                        null,
                        null,
                        FrequencyType.WEEKLY_X_TIMES,
                        null,
                        5,
                        null
                )
        );

        // Берем ID созданной привычки
        List<HabitShortInfoResponse> habitShortInfoResponses1 = habitHelper.getAllUserHabitsAtDay(token1, TODAY_DATE_STR);
        Optional<HabitShortInfoResponse> existingHabitOptional1 = habitShortInfoResponses1.stream()
                .filter(h -> h.getName().equals("Привычка 1"))
                .findFirst();
        assertThat(existingHabitOptional1).isPresent();
        Long existingHabitId1 = existingHabitOptional1.get().getHabitId();

        // У юзера 1 в списке отправленных принятых заявок на созданную привычку пусто
        List<AcceptedSubscriptionForCreatorResponse> acceptedSubscriptionForCreatorResponses1 =
                subscriptionHelper.getHabitAcceptedSubscriptions(token1, existingHabitId1);
        assertThat(acceptedSubscriptionForCreatorResponses1).isEmpty();

        // У юзера 1 в списке отправленных непринятых заявок на созданную привычку пусто
        List<UnprocessedRequestForCreatorResponse> unprocessedRequestForCreatorResponses1 =
                subscriptionHelper.getHabitUnprocessedRequests(token1, existingHabitId1);
        assertThat(unprocessedRequestForCreatorResponses1).isEmpty();

        // У юзера 2 в списке отправленных принятых заявок пусто
        List<AcceptedSubscriptionForSubscriberResponse> acceptedSubscriptionForSubscriberResponses1 =
                subscriptionHelper.getUserAcceptedSubscriptions(token2);
        assertThat(acceptedSubscriptionForSubscriberResponses1).isEmpty();

        // У юзера 2 в списке отправленных непринятых заявок пусто
        List<UnprocessedRequestForSubscriberResponse> unprocessedRequestForSubscriberResponses1 =
                subscriptionHelper.getUserUnprocessedRequests(token2);
        assertThat(unprocessedRequestForSubscriberResponses1).isEmpty();

        // Юзер 2 отправляет заявку на созданную привычку юзера 1
        subscriptionHelper.sendSubscriptionRequest(token2, existingHabitId1);

        // У юзера 1 в списке отправленных принятых заявок на созданную привычку пусто
        List<AcceptedSubscriptionForCreatorResponse> acceptedSubscriptionForCreatorResponses2 =
                subscriptionHelper.getHabitAcceptedSubscriptions(token1, existingHabitId1);
        assertThat(acceptedSubscriptionForCreatorResponses2).isEmpty();

        // У юзера 1 в списке отправленных непринятых заявок на созданную привычку появилась 1 заявка
        List<UnprocessedRequestForCreatorResponse> unprocessedRequestForCreatorResponses2 =
                subscriptionHelper.getHabitUnprocessedRequests(token1, existingHabitId1);
        assertThat(unprocessedRequestForCreatorResponses2.size()).isEqualTo(1);
        assertThat(unprocessedRequestForCreatorResponses2.get(0).getSubscriberLogin()).isEqualTo(userLogin2);

        Long existingSubscriptionId = unprocessedRequestForCreatorResponses2.get(0).getSubscriptionId();

        // У юзера 2 в списке отправленных принятых заявок пусто
        List<AcceptedSubscriptionForSubscriberResponse> acceptedSubscriptionForSubscriberResponses2 =
                subscriptionHelper.getUserAcceptedSubscriptions(token2);
        assertThat(acceptedSubscriptionForSubscriberResponses2).isEmpty();

        // У юзера 2 в списке отправленных непринятых заявок появилась 1 заявка
        List<UnprocessedRequestForSubscriberResponse> unprocessedRequestForSubscriberResponses2 =
                subscriptionHelper.getUserUnprocessedRequests(token2);
        assertThat(unprocessedRequestForSubscriberResponses2.size()).isEqualTo(1);
        assertThat(unprocessedRequestForSubscriberResponses2.get(0).getHabitId()).isEqualTo(existingHabitId1);
        assertThat(unprocessedRequestForSubscriberResponses2.get(0).getHabitName()).isEqualTo("Привычка 1");

        // Юзер 1 отклоняет заявку юзера 2
        subscriptionHelper.denySubscriptionRequest(token1, existingSubscriptionId);

        // У юзера 1 в списке отправленных принятых заявок на созданную привычку пусто
        List<AcceptedSubscriptionForCreatorResponse> acceptedSubscriptionForCreatorResponses3 =
                subscriptionHelper.getHabitAcceptedSubscriptions(token1, existingHabitId1);
        assertThat(acceptedSubscriptionForCreatorResponses3).isEmpty();

        // У юзера 1 в списке отправленных непринятых заявок на созданную привычку пусто
        List<UnprocessedRequestForCreatorResponse> unprocessedRequestForCreatorResponses3 =
                subscriptionHelper.getHabitUnprocessedRequests(token1, existingHabitId1);
        assertThat(unprocessedRequestForCreatorResponses3).isEmpty();

        // У юзера 2 в списке отправленных принятых заявок пусто
        List<AcceptedSubscriptionForSubscriberResponse> acceptedSubscriptionForSubscriberResponses3 =
                subscriptionHelper.getUserAcceptedSubscriptions(token2);
        assertThat(acceptedSubscriptionForSubscriberResponses3).isEmpty();

        // У юзера 2 в списке отправленных непринятых заявок пусто
        List<UnprocessedRequestForSubscriberResponse> unprocessedRequestForSubscriberResponses3 =
                subscriptionHelper.getUserUnprocessedRequests(token2);
        assertThat(unprocessedRequestForSubscriberResponses3).isEmpty();
    }

    @Test
    void testAcceptingSubscriptionRequest() throws InterruptedException {
        // Создается юзер 1 с рандомным логином
        String userLogin1 = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin1, "12345");
        String token1 = authHelper.login(userLogin1, "12345").getToken();

        // Создается юзер 2 с рандомным логином
        String userLogin2 = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin2, "12345");
        String token2 = authHelper.login(userLogin2, "12345").getToken();

        // Юзер 1 создал привычку с WEEKLY_X_TIMES
        habitHelper.createHabit(
                token1,
                new HabitCreationRequest(
                        "Привычка 1",
                        null,
                        null,
                        null,
                        null,
                        FrequencyType.WEEKLY_X_TIMES,
                        null,
                        5,
                        null
                )
        );

        // Берем ID созданной привычки
        List<HabitShortInfoResponse> habitShortInfoResponses1 = habitHelper.getAllUserHabitsAtDay(token1, TODAY_DATE_STR);
        Optional<HabitShortInfoResponse> existingHabitOptional1 = habitShortInfoResponses1.stream()
                .filter(h -> h.getName().equals("Привычка 1"))
                .findFirst();
        assertThat(existingHabitOptional1).isPresent();
        Long existingHabitId1 = existingHabitOptional1.get().getHabitId();

        // У юзера 1 в списке отправленных принятых заявок на созданную привычку пусто
        List<AcceptedSubscriptionForCreatorResponse> acceptedSubscriptionForCreatorResponses1 =
                subscriptionHelper.getHabitAcceptedSubscriptions(token1, existingHabitId1);
        assertThat(acceptedSubscriptionForCreatorResponses1).isEmpty();

        // У юзера 1 в списке отправленных непринятых заявок на созданную привычку пусто
        List<UnprocessedRequestForCreatorResponse> unprocessedRequestForCreatorResponses1 =
                subscriptionHelper.getHabitUnprocessedRequests(token1, existingHabitId1);
        assertThat(unprocessedRequestForCreatorResponses1).isEmpty();

        // У юзера 2 в списке отправленных принятых заявок пусто
        List<AcceptedSubscriptionForSubscriberResponse> acceptedSubscriptionForSubscriberResponses1 =
                subscriptionHelper.getUserAcceptedSubscriptions(token2);
        assertThat(acceptedSubscriptionForSubscriberResponses1).isEmpty();

        // У юзера 2 в списке отправленных непринятых заявок пусто
        List<UnprocessedRequestForSubscriberResponse> unprocessedRequestForSubscriberResponses1 =
                subscriptionHelper.getUserUnprocessedRequests(token2);
        assertThat(unprocessedRequestForSubscriberResponses1).isEmpty();

        // Юзер 2 отправляет заявку на созданную привычку юзера 1
        subscriptionHelper.sendSubscriptionRequest(token2, existingHabitId1);

        // У юзера 1 в списке отправленных принятых заявок на созданную привычку пусто
        List<AcceptedSubscriptionForCreatorResponse> acceptedSubscriptionForCreatorResponses2 =
                subscriptionHelper.getHabitAcceptedSubscriptions(token1, existingHabitId1);
        assertThat(acceptedSubscriptionForCreatorResponses2).isEmpty();

        // У юзера 1 в списке отправленных непринятых заявок на созданную привычку появилась 1 заявка
        List<UnprocessedRequestForCreatorResponse> unprocessedRequestForCreatorResponses2 =
                subscriptionHelper.getHabitUnprocessedRequests(token1, existingHabitId1);
        assertThat(unprocessedRequestForCreatorResponses2.size()).isEqualTo(1);
        assertThat(unprocessedRequestForCreatorResponses2.get(0).getSubscriberLogin()).isEqualTo(userLogin2);

        Long existingSubscriptionId = unprocessedRequestForCreatorResponses2.get(0).getSubscriptionId();

        // У юзера 2 в списке отправленных принятых заявок пусто
        List<AcceptedSubscriptionForSubscriberResponse> acceptedSubscriptionForSubscriberResponses2 =
                subscriptionHelper.getUserAcceptedSubscriptions(token2);
        assertThat(acceptedSubscriptionForSubscriberResponses2).isEmpty();

        // У юзера 2 в списке отправленных непринятых заявок появилась 1 заявка
        List<UnprocessedRequestForSubscriberResponse> unprocessedRequestForSubscriberResponses2 =
                subscriptionHelper.getUserUnprocessedRequests(token2);
        assertThat(unprocessedRequestForSubscriberResponses2.size()).isEqualTo(1);
        assertThat(unprocessedRequestForSubscriberResponses2.get(0).getHabitId()).isEqualTo(existingHabitId1);
        assertThat(unprocessedRequestForSubscriberResponses2.get(0).getHabitName()).isEqualTo("Привычка 1");

        // Юзер 1 принимает заявку юзера 2
        subscriptionHelper.acceptSubscriptionRequest(token1, existingSubscriptionId);

        // У юзера 1 в списке отправленных принятых заявок на созданную привычку появилась 1 подписка
        List<AcceptedSubscriptionForCreatorResponse> acceptedSubscriptionForCreatorResponses3 =
                subscriptionHelper.getHabitAcceptedSubscriptions(token1, existingHabitId1);
        assertThat(acceptedSubscriptionForCreatorResponses3.size()).isEqualTo(1);
        assertThat(acceptedSubscriptionForCreatorResponses3.get(0).getSubscriberLogin()).isEqualTo(userLogin2);

        // У юзера 1 в списке отправленных непринятых заявок на созданную привычку пусто
        List<UnprocessedRequestForCreatorResponse> unprocessedRequestForCreatorResponses3 =
                subscriptionHelper.getHabitUnprocessedRequests(token1, existingHabitId1);
        assertThat(unprocessedRequestForCreatorResponses3).isEmpty();

        // У юзера 2 в списке отправленных принятых заявок появилась 1 подписка
        List<AcceptedSubscriptionForSubscriberResponse> acceptedSubscriptionForSubscriberResponses3 =
                subscriptionHelper.getUserAcceptedSubscriptions(token2);
        assertThat(acceptedSubscriptionForSubscriberResponses3.size()).isEqualTo(1);
        assertThat(acceptedSubscriptionForSubscriberResponses3.get(0).getHabitName()).isEqualTo("Привычка 1");
        assertThat(acceptedSubscriptionForSubscriberResponses3.get(0).getHabitId()).isEqualTo(existingHabitId1);

        // У юзера 2 в списке отправленных непринятых заявок пусто
        List<UnprocessedRequestForSubscriberResponse> unprocessedRequestForSubscriberResponses3 =
                subscriptionHelper.getUserUnprocessedRequests(token2);
        assertThat(unprocessedRequestForSubscriberResponses3).isEmpty();
    }

    @Test
    void testUnsubscribing() throws InterruptedException {
        // Создается юзер 1 с рандомным логином
        String userLogin1 = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin1, "12345");
        String token1 = authHelper.login(userLogin1, "12345").getToken();

        // Создается юзер 2 с рандомным логином
        String userLogin2 = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin2, "12345");
        String token2 = authHelper.login(userLogin2, "12345").getToken();

        // Юзер 1 создал привычку с WEEKLY_X_TIMES
        habitHelper.createHabit(
                token1,
                new HabitCreationRequest(
                        "Привычка 1",
                        null,
                        null,
                        null,
                        null,
                        FrequencyType.WEEKLY_X_TIMES,
                        null,
                        5,
                        null
                )
        );

        // Берем ID созданной привычки
        List<HabitShortInfoResponse> habitShortInfoResponses1 = habitHelper.getAllUserHabitsAtDay(token1, TODAY_DATE_STR);
        Optional<HabitShortInfoResponse> existingHabitOptional1 = habitShortInfoResponses1.stream()
                .filter(h -> h.getName().equals("Привычка 1"))
                .findFirst();
        assertThat(existingHabitOptional1).isPresent();
        Long existingHabitId1 = existingHabitOptional1.get().getHabitId();

        // Юзер 2 отправляет заявку на созданную привычку юзера 1
        subscriptionHelper.sendSubscriptionRequest(token2, existingHabitId1);

        List<UnprocessedRequestForCreatorResponse> unprocessedRequestForCreatorResponses =
                subscriptionHelper.getHabitUnprocessedRequests(token1, existingHabitId1);
        Long existingSubscriptionId = unprocessedRequestForCreatorResponses.get(0).getSubscriptionId();

        // Юзер 1 принимает заявку юзера 2
        subscriptionHelper.acceptSubscriptionRequest(token1, existingSubscriptionId);

        // Юзер 2 отписывается от привычки юзера 1
        subscriptionHelper.unsubscribe(token2, existingHabitId1);

        // У юзера 2 больше нет привычки юзера 1 в списке текущих
        List<SubscribedHabitShortInfoResponse> subscribedHabitShortInfoResponses =
                habitHelper.getAllUserSubscribedHabitsAtDay(token2, TODAY_DATE_STR);
        assertThat(subscribedHabitShortInfoResponses).isEmpty();

        // У юзера 1 больше нет подписки юзера 2 среди подписок на его привычку
        List<AcceptedSubscriptionForCreatorResponse> acceptedSubscriptionForCreatorResponses =
                subscriptionHelper.getHabitAcceptedSubscriptions(token1, existingHabitId1);
        assertThat(acceptedSubscriptionForCreatorResponses).isEmpty();

        // У юзера 2 больше нет подписки на привычку юзера 1 среди его подписок
        List<AcceptedSubscriptionForSubscriberResponse> acceptedSubscriptionForSubscriberResponses =
                subscriptionHelper.getUserAcceptedSubscriptions(token2);
        assertThat(acceptedSubscriptionForSubscriberResponses).isEmpty();
    }

    @Test
    void testGettingHabitInfoForSubscriber() throws InterruptedException {
        // Создается юзер 1 с рандомным логином
        String userLogin1 = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin1, "12345");
        String token1 = authHelper.login(userLogin1, "12345").getToken();

        // Создается юзер 2 с рандомным логином
        String userLogin2 = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin2, "12345");
        String token2 = authHelper.login(userLogin2, "12345").getToken();

        // Юзер 1 создал привычку с WEEKLY_X_TIMES
        habitHelper.createHabit(
                token1,
                new HabitCreationRequest(
                        "Привычка 1",
                        null,
                        null,
                        null,
                        null,
                        FrequencyType.WEEKLY_X_TIMES,
                        null,
                        5,
                        null
                )
        );

        // Берем ID созданной привычки
        List<HabitShortInfoResponse> habitShortInfoResponses1 = habitHelper.getAllUserHabitsAtDay(token1, TODAY_DATE_STR);
        Optional<HabitShortInfoResponse> existingHabitOptional1 = habitShortInfoResponses1.stream()
                .filter(h -> h.getName().equals("Привычка 1"))
                .findFirst();
        assertThat(existingHabitOptional1).isPresent();
        Long existingHabitId1 = existingHabitOptional1.get().getHabitId();

        // Юзер 2 отправляет заявку на привычку юзера 1
        subscriptionHelper.sendSubscriptionRequest(token2, existingHabitId1);

        // У юзера 2 среди текущих подписанных привычек пока нет привычек
        List<SubscribedHabitShortInfoResponse> subscribedHabitShortInfoResponses1 =
                habitHelper.getAllUserSubscribedHabitsAtDay(token2, TODAY_DATE_STR);
        assertThat(subscribedHabitShortInfoResponses1.size()).isEqualTo(0);

        Long existingSubscriptionId = subscriptionHelper.getHabitUnprocessedRequests(token1, existingHabitId1).get(0).getSubscriptionId();

        // Юзер 1 принимает заявку юзера 2
        subscriptionHelper.acceptSubscriptionRequest(token1, existingSubscriptionId);

        // У юзера 2 среди текущих подписанных привычек появилась привычку юзера 1
        List<SubscribedHabitShortInfoResponse> subscribedHabitShortInfoResponses2 =
                habitHelper.getAllUserSubscribedHabitsAtDay(token2, TODAY_DATE_STR);
        assertThat(subscribedHabitShortInfoResponses2.size()).isEqualTo(1);
        SubscribedHabitShortInfoResponse subscribedHabitShortInfoResponse = subscribedHabitShortInfoResponses2.get(0);
        assertThat(subscribedHabitShortInfoResponse.getHabitId()).isEqualTo(existingHabitId1);
        assertThat(subscribedHabitShortInfoResponse.getCreatorLogin()).isEqualTo(userLogin1);
        assertThat(subscribedHabitShortInfoResponse.getName()).isEqualTo("Привычка 1");
        assertThat(subscribedHabitShortInfoResponse.getSubscribersCount()).isEqualTo(1);
        assertThat(subscribedHabitShortInfoResponse.getIsCompleted()).isEqualTo(false);
        assertThat(subscribedHabitShortInfoResponse.getFrequencyType()).isEqualTo(FrequencyType.WEEKLY_X_TIMES);
        assertThat(subscribedHabitShortInfoResponse.getCompletionsInPeriod()).isEqualTo(0);
        assertThat(subscribedHabitShortInfoResponse.getCompletionsPlannedInPeriod()).isEqualTo(5);
        assertThat(subscribedHabitShortInfoResponse.getIsPhotoAllowed()).isEqualTo(false);
        assertThat(subscribedHabitShortInfoResponse.getIsPhotoUploaded()).isEqualTo(false);

        // Юзер 2 может смотреть общую инфу о привычке юзера 1
        HabitGeneralInfoResponse habitGeneralInfoResponse = habitHelper.getGeneralInfo(token2, existingHabitId1);
        assertThat(habitGeneralInfoResponse.getName()).isEqualTo("Привычка 1");

        // Юзер 2 может смотреть инфу по отчетам о привычке юзера 1
        HabitReportsInfoResponse habitReportsInfoResponse = habitHelper.getReportsInfo(token2, existingHabitId1);
        assertThat(habitReportsInfoResponse.getCompletionsInTotal()).isEqualTo(0);

        // Юзер 2 может смотреть отчет за конкретный о привычке юзера 1
        ReportFullInfoResponse reportFullInfoResponse = habitHelper.getReportAtDay(token2, existingHabitId1, TODAY_DATE_STR);
        assertThat(reportFullInfoResponse.isCompleted()).isEqualTo(false);
    }

}
