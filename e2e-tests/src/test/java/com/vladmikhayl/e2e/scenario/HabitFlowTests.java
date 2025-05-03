package com.vladmikhayl.e2e.scenario;

import com.vladmikhayl.e2e.BaseE2ETest;
import com.vladmikhayl.e2e.dto.habit.HabitCreationRequest;
import com.vladmikhayl.e2e.dto.habit.HabitEditingRequest;
import com.vladmikhayl.e2e.dto.habit.HabitGeneralInfoResponse;
import com.vladmikhayl.e2e.dto.habit.HabitShortInfoResponse;
import com.vladmikhayl.e2e.dto.subscription.UnprocessedRequestForSubscriberResponse;
import com.vladmikhayl.e2e.entity.FrequencyType;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HabitFlowTests extends BaseE2ETest {

    @Test
    void testHabitCreatingAndGettingAllUserHabitsAtDay() throws InterruptedException {
        // Создается юзер с рандомным логином
        String userLogin = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin, "12345");
        String token = authHelper.login(userLogin, "12345").getToken();

        // Привычки юзера на сегодняшний день (должно быть 0)
        List<HabitShortInfoResponse> habitShortInfoResponses1 = habitHelper.getAllUserHabitsAtDay(token, TODAY_DATE_STR);
        assertThat(habitShortInfoResponses1.size()).isEqualTo(0);

        // Юзер создал привычку с WEEKLY_X_TIMES
        habitHelper.createHabit(
                token,
                new HabitCreationRequest(
                        "Привычка 1",
                        null,
                        null,
                        null,
                        FrequencyType.WEEKLY_X_TIMES,
                        null,
                        5,
                        null
                )
        );

        // Привычки юзера на сегодняшний день (должно быть 1)
        List<HabitShortInfoResponse> habitShortInfoResponses2 = habitHelper.getAllUserHabitsAtDay(token, TODAY_DATE_STR);
        assertThat(habitShortInfoResponses2.size()).isEqualTo(1);

        Optional<HabitShortInfoResponse> existingHabitOptional1 = habitShortInfoResponses2.stream()
                .filter(h -> h.getName().equals("Привычка 1"))
                .findFirst();
        assertThat(existingHabitOptional1).isPresent();

        HabitShortInfoResponse existingHabit1 = existingHabitOptional1.get();
        assertThat(existingHabit1.getIsCompleted()).isEqualTo(false);
        assertThat(existingHabit1.getSubscribersCount()).isEqualTo(0);
        assertThat(existingHabit1.getFrequencyType()).isEqualTo(FrequencyType.WEEKLY_X_TIMES);
        assertThat(existingHabit1.getCompletionsInPeriod()).isEqualTo(0);
        assertThat(existingHabit1.getCompletionsPlannedInPeriod()).isEqualTo(5);
        assertThat(existingHabit1.getIsPhotoAllowed()).isEqualTo(false);
        assertThat(existingHabit1.getIsPhotoUploaded()).isEqualTo(false);
        assertThat(existingHabit1.getReportId()).isEqualTo(null);

        // Юзер создал привычку с WEEKLY_ON_DAYS, которая выполняется только в завтрашний день недели
        habitHelper.createHabit(
                token,
                new HabitCreationRequest(
                        "Привычка 2",
                        "Описание",
                        true,
                        null,
                        FrequencyType.WEEKLY_ON_DAYS,
                        Set.of(LocalDate.now().plusDays(1).getDayOfWeek()),
                        null,
                        null
                )
        );

        // Привычки юзера на сегодняшний день (должно быть 1)
        List<HabitShortInfoResponse> habitShortInfoResponses3 = habitHelper.getAllUserHabitsAtDay(token, TODAY_DATE_STR);
        assertThat(habitShortInfoResponses3.size()).isEqualTo(1);

        // Привычки юзера на завтрашний день (должно быть 2)
        List<HabitShortInfoResponse> habitShortInfoResponses4 = habitHelper.getAllUserHabitsAtDay(token, TOMORROW_DATE_STR);
        assertThat(habitShortInfoResponses4.size()).isEqualTo(2);

        Optional<HabitShortInfoResponse> existingHabitOptional2 = habitShortInfoResponses4.stream()
                .filter(h -> h.getName().equals("Привычка 2"))
                .findFirst();
        assertThat(existingHabitOptional2).isPresent();

        HabitShortInfoResponse existingHabit2 = existingHabitOptional2.get();
        assertThat(existingHabit2.getIsCompleted()).isEqualTo(false);
        assertThat(existingHabit2.getSubscribersCount()).isEqualTo(0);
        assertThat(existingHabit2.getFrequencyType()).isEqualTo(FrequencyType.WEEKLY_ON_DAYS);
        assertThat(existingHabit2.getCompletionsInPeriod()).isEqualTo(null);
        assertThat(existingHabit2.getCompletionsPlannedInPeriod()).isEqualTo(null);
        assertThat(existingHabit2.getIsPhotoAllowed()).isEqualTo(true);
        assertThat(existingHabit2.getIsPhotoUploaded()).isEqualTo(false);
        assertThat(existingHabit2.getReportId()).isEqualTo(null);

        // Привычки юзера на вчерашний день (должно быть 0)
        List<HabitShortInfoResponse> habitShortInfoResponses5 = habitHelper.getAllUserHabitsAtDay(token, YESTERDAY_DATE_STR);
        assertThat(habitShortInfoResponses5.size()).isEqualTo(0);
    }

    @Test
    void testHabitEditing() throws InterruptedException {
        // Создается юзер с рандомным логином
        String userLogin = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin, "12345");
        String token = authHelper.login(userLogin, "12345").getToken();

        // Юзер создал привычку с WEEKLY_ON_DAYS, которая выполняется только в сегодняшний день недели
        habitHelper.createHabit(
                token,
                new HabitCreationRequest(
                        "Привычка 1",
                        "Старое описание",
                        false,
                        null,
                        FrequencyType.WEEKLY_ON_DAYS,
                        Set.of(LocalDate.now().getDayOfWeek()),
                        null,
                        null
                )
        );

        // Берем ID созданной привычки
        List<HabitShortInfoResponse> habitShortInfoResponses = habitHelper.getAllUserHabitsAtDay(token, TODAY_DATE_STR);
        Optional<HabitShortInfoResponse> existingHabitOptional = habitShortInfoResponses.stream()
                .filter(h -> h.getName().equals("Привычка 1"))
                .findFirst();
        assertThat(existingHabitOptional).isPresent();
        Long existingHabitId = existingHabitOptional.get().getHabitId();

        // Проверяем общую инфу по созданной привычке
        HabitGeneralInfoResponse habitGeneralInfoResponse1 = habitHelper.getGeneralInfo(token, existingHabitId);
        assertThat(habitGeneralInfoResponse1.getDescription()).isEqualTo("Старое описание");
//        assertThat(habitGeneralInfoResponse1.getIsHarmful()).isEqualTo(false);
        assertThat(habitGeneralInfoResponse1.getDurationDays()).isEqualTo(null);

        // Юзер редактирует свою привычку
        habitHelper.editHabit(
                token,
                existingHabitId,
                new HabitEditingRequest(
                        "Новое описание",
                        30
                )
        );

        // Проверяем общую инфу по измененной привычке
        HabitGeneralInfoResponse habitGeneralInfoResponse2 = habitHelper.getGeneralInfo(token, existingHabitId);
        assertThat(habitGeneralInfoResponse2.getDescription()).isEqualTo("Новое описание");
//        assertThat(habitGeneralInfoResponse2.getIsHarmful()).isEqualTo(true);
        assertThat(habitGeneralInfoResponse2.getDurationDays()).isEqualTo(30);

        // Юзер редактирует свою привычку
        habitHelper.editHabit(
                token,
                existingHabitId,
                new HabitEditingRequest(
                        null,
                        0
                )
        );

        // Проверяем общую инфу по измененной привычке
        HabitGeneralInfoResponse habitGeneralInfoResponse3 = habitHelper.getGeneralInfo(token, existingHabitId);
        assertThat(habitGeneralInfoResponse3.getDescription()).isEqualTo("Новое описание");
//        assertThat(habitGeneralInfoResponse3.getIsHarmful()).isEqualTo(true);
        assertThat(habitGeneralInfoResponse3.getDurationDays()).isEqualTo(null);
    }

    @Test
    void testHabitDeleting() throws InterruptedException {
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

        // Юзер 1 создал привычку с WEEKLY_ON_DAYS, которая выполняется только в сегодняшний день недели
        habitHelper.createHabit(
                token1,
                new HabitCreationRequest(
                        "Привычка 1",
                        null,
                        false,
                        null,
                        FrequencyType.WEEKLY_ON_DAYS,
                        Set.of(LocalDate.now().getDayOfWeek()),
                        null,
                        null
                )
        );

        // Проверяем, что в списке текущих на сегодня привычек появилась привычки, и берем ее ID
        List<HabitShortInfoResponse> habitShortInfoResponses = habitHelper.getAllUserHabitsAtDay(token1, TODAY_DATE_STR);
        assertThat(habitShortInfoResponses.size()).isEqualTo(1);
        Optional<HabitShortInfoResponse> existingHabitOptional = habitShortInfoResponses.stream()
                .filter(h -> h.getName().equals("Привычка 1"))
                .findFirst();
        assertThat(existingHabitOptional).isPresent();
        Long existingHabitId = existingHabitOptional.get().getHabitId();

        // Юзер 2 отправляет заявку на привычку юзера 1
        subscriptionHelper.sendSubscriptionRequest(token2, existingHabitId);

        // Юзер 1 удаляет созданную привычку
        habitHelper.deleteHabit(token1, existingHabitId);

        // Проверяем, что в списке текущих на сегодня привычек больше нет привычек
        List<HabitShortInfoResponse> habitShortInfoResponsesAfterDeleting = habitHelper.getAllUserHabitsAtDay(token1, TODAY_DATE_STR);
        assertThat(habitShortInfoResponsesAfterDeleting.size()).isEqualTo(0);

        // Проверяем, что у юзера 2 пропала заявка на удаленную привычку
        List<UnprocessedRequestForSubscriberResponse> unprocessedRequestForSubscriberResponses2 =
                subscriptionHelper.getUserUnprocessedRequests(token2);
        assertThat(unprocessedRequestForSubscriberResponses2).isEmpty();

        // Проверяем, что юзер 3 не может отправить заявку на удаленную привычку
        assertThatThrownBy(() -> subscriptionHelper.sendSubscriptionRequest(token3, existingHabitId))
                .isInstanceOf(HttpClientErrorException.NotFound.class)
                .hasMessageContaining("Привычка не найдена");
    }

}
