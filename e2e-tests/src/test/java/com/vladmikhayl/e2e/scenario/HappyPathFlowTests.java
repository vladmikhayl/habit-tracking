package com.vladmikhayl.e2e.scenario;

import com.vladmikhayl.e2e.BaseE2ETest;
import com.vladmikhayl.e2e.dto.habit.HabitCreationRequest;
import com.vladmikhayl.e2e.dto.habit.HabitShortInfoResponse;
import com.vladmikhayl.e2e.dto.habit.ReportFullInfoResponse;
import com.vladmikhayl.e2e.dto.habit.SubscribedHabitShortInfoResponse;
import com.vladmikhayl.e2e.dto.report.ReportCreationRequest;
import com.vladmikhayl.e2e.dto.report.ReportPhotoEditingRequest;
import com.vladmikhayl.e2e.dto.subscription.AcceptedSubscriptionForSubscriberResponse;
import com.vladmikhayl.e2e.dto.subscription.UnprocessedRequestForCreatorResponse;
import com.vladmikhayl.e2e.entity.FrequencyType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class HappyPathFlowTests extends BaseE2ETest {

    @Test
    void testHappyPathForThreeUsers() throws InterruptedException {
        // Создается юзер 1 с рандомным логином
        String userLogin1 = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin1, "12345");
        String token1 = authHelper.login(userLogin1, "12345").getToken();

        // Создается юзер 2 с рандомным логином
        String userLogin2 = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin2, "12345");
        String token2 = authHelper.login(userLogin2, "12345").getToken();

        // Создается юзер 3 с рандомным логином
        String userLogin3 = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin3, "12345");
        String token3 = authHelper.login(userLogin3, "12345").getToken();

        // Юзер 1 создал привычку с MONTHLY_X_TIMES
        habitHelper.createHabit(
                token1,
                new HabitCreationRequest(
                        "Привычка 1",
                        "Описание",
                        true,
                        false,
                        null,
                        FrequencyType.MONTHLY_X_TIMES,
                        null,
                        null,
                        10
                )
        );

        // Юзер 1 смотрит привычки на сегодняшний день (проверяем, что там 1 привычка)
        List<HabitShortInfoResponse> habitShortInfoResponses1 = habitHelper.getAllUserHabitsAtDay(token1, TODAY_DATE_STR);
        assertThat(habitShortInfoResponses1.size()).isEqualTo(1);

        Long habitId1 = habitShortInfoResponses1.get(0).getHabitId();

        // Юзер 1 создал отчет о своей привычке
        reportHelper.createReport(
                token1,
                new ReportCreationRequest(
                        habitId1,
                        LocalDate.now(),
                        null
                )
        );

        // Юзер 1 может посмотреть свой созданный отчет
        ReportFullInfoResponse reportFullInfoResponse1 = habitHelper.getReportAtDay(token1, habitId1, TODAY_DATE_STR);
        assertThat(reportFullInfoResponse1.isCompleted()).isTrue();
        assertThat(reportFullInfoResponse1.getPhotoUrl()).isNull();

        Long reportId = reportFullInfoResponse1.getReportId();

        // Юзер 1 может прикрепить фото к созданному отчету
        reportHelper.changeReportPhoto(
                token1,
                reportId,
                new ReportPhotoEditingRequest("https://i.pinimg.com/736x/b9/a7/55/b9a75516248779bead50d84c52daebf3.jpg")
        );

        // Юзер 1 может посмотреть свой измененный отчет
        ReportFullInfoResponse reportFullInfoResponse2 = habitHelper.getReportAtDay(token1, habitId1, TODAY_DATE_STR);
        assertThat(reportFullInfoResponse2.getPhotoUrl()).isEqualTo("https://i.pinimg.com/736x/b9/a7/55/b9a75516248779bead50d84c52daebf3.jpg");

        // Юзер 2 отправляет заявку на привычку юзера 1
        subscriptionHelper.sendSubscriptionRequest(token2, habitId1);

        // Юзер 1 получил заявку от юзера 2
        List<UnprocessedRequestForCreatorResponse> unprocessedRequestForCreatorResponses1 =
                subscriptionHelper.getHabitUnprocessedRequests(token1, habitId1);
        assertThat(unprocessedRequestForCreatorResponses1.size()).isEqualTo(1);

        Long subscriptionId1 = unprocessedRequestForCreatorResponses1.get(0).getSubscriptionId();

        // Юзер 1 принимает заявку юзера 2
        subscriptionHelper.acceptSubscriptionRequest(token1, subscriptionId1);

        // Юзер 1 смотрит подписанные привычки на сегодняшний день (проверяем, что там 0 привычек)
        List<SubscribedHabitShortInfoResponse> subscribedHabitShortInfoResponses1 =
                habitHelper.getAllUserSubscribedHabitsAtDay(token1, TODAY_DATE_STR);
        assertThat(subscribedHabitShortInfoResponses1.size()).isEqualTo(0);

        // Юзер 2 смотрит подписанные привычки на сегодняшний день (проверяем, что там 1 привычка)
        List<SubscribedHabitShortInfoResponse> subscribedHabitShortInfoResponses2 =
                habitHelper.getAllUserSubscribedHabitsAtDay(token2, TODAY_DATE_STR);
        assertThat(subscribedHabitShortInfoResponses2.size()).isEqualTo(1);

        // Юзер 2 смотрит подписанные привычки на завтрашний день (проверяем, что там 1 привычка)
        List<SubscribedHabitShortInfoResponse> subscribedHabitShortInfoResponses3 =
                habitHelper.getAllUserSubscribedHabitsAtDay(token2, TOMORROW_DATE_STR);
        assertThat(subscribedHabitShortInfoResponses3.size()).isEqualTo(1);

        // Юзер 3 создал привычку с WEEKLY_ON_DAYS, которая выполняется только в завтрашний день недели
        habitHelper.createHabit(
                token3,
                new HabitCreationRequest(
                        "Привычка 1",
                        "Описание",
                        false,
                        true,
                        null,
                        FrequencyType.WEEKLY_ON_DAYS,
                        Set.of(LocalDate.now().plusDays(1).getDayOfWeek()),
                        null,
                        null
                )
        );

        // Юзер 3 смотрит привычки на завтрашний день (проверяем, что там 1 привычка)
        List<HabitShortInfoResponse> habitShortInfoResponses2 = habitHelper.getAllUserHabitsAtDay(token3, TOMORROW_DATE_STR);
        assertThat(habitShortInfoResponses2.size()).isEqualTo(1);

        Long habitId2 = habitShortInfoResponses2.get(0).getHabitId();

        // Юзер 2 отправляет заявку на привычку юзера 3
        subscriptionHelper.sendSubscriptionRequest(token2, habitId2);

        // Юзер 3 получил заявку от юзера 2
        List<UnprocessedRequestForCreatorResponse> unprocessedRequestForCreatorResponses2 =
                subscriptionHelper.getHabitUnprocessedRequests(token3, habitId2);
        assertThat(unprocessedRequestForCreatorResponses2.size()).isEqualTo(1);

        Long subscriptionId2 = unprocessedRequestForCreatorResponses2.get(0).getSubscriptionId();

        // Юзер 3 принимает заявку юзера 2
        subscriptionHelper.acceptSubscriptionRequest(token3, subscriptionId2);

        // Юзер 2 смотрит подписанные привычки на сегодняшний день (проверяем, что там 1 привычка)
        List<SubscribedHabitShortInfoResponse> subscribedHabitShortInfoResponses4 =
                habitHelper.getAllUserSubscribedHabitsAtDay(token2, TODAY_DATE_STR);
        assertThat(subscribedHabitShortInfoResponses4.size()).isEqualTo(1);

        // Юзер 2 смотрит подписанные привычки на завтрашний день (проверяем, что там 2 привычки)
        List<SubscribedHabitShortInfoResponse> subscribedHabitShortInfoResponses5 =
                habitHelper.getAllUserSubscribedHabitsAtDay(token2, TOMORROW_DATE_STR);
        assertThat(subscribedHabitShortInfoResponses5.size()).isEqualTo(2);

        // Юзер 2 смотрит свои принятые подписки (проверяем, что там 2 подписки)
        List<AcceptedSubscriptionForSubscriberResponse> acceptedSubscriptionForSubscriberResponses =
                subscriptionHelper.getUserAcceptedSubscriptions(token2);
        assertThat(acceptedSubscriptionForSubscriberResponses.size()).isEqualTo(2);

        // Юзер 2 отписывается от привычки юзера 3
        subscriptionHelper.unsubscribe(token2, habitId2);

        // Юзер 2 смотрит подписанные привычки на завтрашний день (проверяем, что там 1 привычка)
        List<SubscribedHabitShortInfoResponse> subscribedHabitShortInfoResponses6 =
                habitHelper.getAllUserSubscribedHabitsAtDay(token2, TOMORROW_DATE_STR);
        assertThat(subscribedHabitShortInfoResponses6.size()).isEqualTo(1);
    }

}
