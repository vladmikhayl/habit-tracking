package com.vladmikhayl.e2e.scenario;

import com.vladmikhayl.e2e.BaseE2ETest;
import com.vladmikhayl.e2e.dto.habit.HabitCreationRequest;
import com.vladmikhayl.e2e.dto.habit.HabitShortInfoResponse;
import com.vladmikhayl.e2e.entity.FrequencyType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateHabitFlowTest extends BaseE2ETest {

    @Test
    void testGettingAllUserHabitsAtDay() {
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

        // Юзер создал привычку с WEEKLY_ON_DAYS
        habitHelper.createHabit(
                token,
                new HabitCreationRequest(
                        "Привычка 2",
                        "Описание",
                        true,
                        true,
                        null,
                        FrequencyType.WEEKLY_ON_DAYS,
                        Set.of(DayOfWeek.WEDNESDAY),
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

        // Привычки юзера на вчерашний день (должно быть 0)
        List<HabitShortInfoResponse> habitShortInfoResponses5 = habitHelper.getAllUserHabitsAtDay(token, YESTERDAY_DATE_STR);
        assertThat(habitShortInfoResponses5.size()).isEqualTo(0);
    }

}
