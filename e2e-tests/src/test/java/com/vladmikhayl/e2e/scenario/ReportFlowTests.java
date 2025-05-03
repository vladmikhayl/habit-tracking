package com.vladmikhayl.e2e.scenario;

import com.vladmikhayl.e2e.BaseE2ETest;
import com.vladmikhayl.e2e.dto.habit.HabitCreationRequest;
import com.vladmikhayl.e2e.dto.habit.HabitReportsInfoResponse;
import com.vladmikhayl.e2e.dto.habit.HabitShortInfoResponse;
import com.vladmikhayl.e2e.dto.habit.ReportFullInfoResponse;
import com.vladmikhayl.e2e.dto.report.ReportCreationRequest;
import com.vladmikhayl.e2e.dto.report.ReportPhotoEditingRequest;
import com.vladmikhayl.e2e.entity.FrequencyType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportFlowTests extends BaseE2ETest {

    @Test
    void testReportCreatingAndGettingReportAtDay() throws InterruptedException {
        // Создается юзер с рандомным логином
        String userLogin = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin, "12345");
        String token = authHelper.login(userLogin, "12345").getToken();

        // Юзер создал привычку с WEEKLY_X_TIMES
        habitHelper.createHabit(
                token,
                new HabitCreationRequest(
                        "Привычка 1",
                        null,
                        false,
                        null,
                        FrequencyType.WEEKLY_X_TIMES,
                        null,
                        5,
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

        // Юзер получает отчет о привычке за сегодня (проверяем, что его нет)
        ReportFullInfoResponse reportFullInfoResponse1 = habitHelper.getReportAtDay(token, existingHabitId, TODAY_DATE_STR);
        assertThat(reportFullInfoResponse1.getReportId()).isNull();
        assertThat(reportFullInfoResponse1.isCompleted()).isFalse();
        assertThat(reportFullInfoResponse1.getCompletionTime()).isNull();
        assertThat(reportFullInfoResponse1.getPhotoUrl()).isNull();

        // Юзер создает отчет на сегодня
        reportHelper.createReport(
                token,
                new ReportCreationRequest(
                        existingHabitId,
                        LocalDate.now(),
                        null
                )
        );

        // Юзер получает отчет о привычке за сегодня (проверяем, что он появился)
        ReportFullInfoResponse reportFullInfoResponse2 = habitHelper.getReportAtDay(token, existingHabitId, TODAY_DATE_STR);
        assertThat(reportFullInfoResponse2.getReportId()).isNotNull();
        assertThat(reportFullInfoResponse2.isCompleted()).isTrue();
        assertThat(reportFullInfoResponse2.getCompletionTime()).isNotNull();
        assertThat(reportFullInfoResponse2.getPhotoUrl()).isNull();

        // Юзер получает отчет о привычке за завтра (проверяем, что его нет)
        ReportFullInfoResponse reportFullInfoResponse3 = habitHelper.getReportAtDay(token, existingHabitId, TOMORROW_DATE_STR);
        assertThat(reportFullInfoResponse3.getReportId()).isNull();
        assertThat(reportFullInfoResponse3.isCompleted()).isFalse();
        assertThat(reportFullInfoResponse3.getCompletionTime()).isNull();
        assertThat(reportFullInfoResponse3.getPhotoUrl()).isNull();
    }

    @Test
    void testReportPhotoChanging() throws InterruptedException {
        // Создается юзер с рандомным логином
        String userLogin = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin, "12345");
        String token = authHelper.login(userLogin, "12345").getToken();

        // Юзер создал привычку с WEEKLY_X_TIMES, которая предусматривает фотоотчет
        habitHelper.createHabit(
                token,
                new HabitCreationRequest(
                        "Привычка 1",
                        null,
                        true,
                        null,
                        FrequencyType.WEEKLY_X_TIMES,
                        null,
                        5,
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

        // Юзер создает отчет на сегодня без фото
        reportHelper.createReport(
                token,
                new ReportCreationRequest(
                        existingHabitId,
                        LocalDate.now(),
                        null
                )
        );

        // Юзер получает отчет о привычке за сегодня (проверяем, что он без фото)
        ReportFullInfoResponse reportFullInfoResponse1 = habitHelper.getReportAtDay(token, existingHabitId, TODAY_DATE_STR);
        assertThat(reportFullInfoResponse1.getReportId()).isNotNull();
        assertThat(reportFullInfoResponse1.isCompleted()).isTrue();
        assertThat(reportFullInfoResponse1.getCompletionTime()).isNotNull();
        assertThat(reportFullInfoResponse1.getPhotoUrl()).isNull();

        Long existingReportId = reportFullInfoResponse1.getReportId();

        // Юзер добавляет к отчету фото
        reportHelper.changeReportPhoto(
                token,
                existingReportId,
                new ReportPhotoEditingRequest(
                        "https://i.pinimg.com/736x/b9/a7/55/b9a75516248779bead50d84c52daebf3.jpg"
                )
        );

        // Юзер получает отчет о привычке за сегодня (проверяем, что он с фото)
        ReportFullInfoResponse reportFullInfoResponse2 = habitHelper.getReportAtDay(token, existingHabitId, TODAY_DATE_STR);
        assertThat(reportFullInfoResponse2.getPhotoUrl()).isEqualTo("https://i.pinimg.com/736x/b9/a7/55/b9a75516248779bead50d84c52daebf3.jpg");

        // Юзер удаляет из отчета фото
        reportHelper.changeReportPhoto(
                token,
                existingReportId,
                new ReportPhotoEditingRequest(
                        ""
                )
        );

        // Юзер получает отчет о привычке за сегодня (проверяем, что он без фото)
        ReportFullInfoResponse reportFullInfoResponse3 = habitHelper.getReportAtDay(token, existingHabitId, TODAY_DATE_STR);
        assertThat(reportFullInfoResponse3.getPhotoUrl()).isNull();
    }

    @Test
    void testReportDeleting() throws InterruptedException {
        // Создается юзер с рандомным логином
        String userLogin = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin, "12345");
        String token = authHelper.login(userLogin, "12345").getToken();

        // Юзер создал привычку с WEEKLY_X_TIMES
        habitHelper.createHabit(
                token,
                new HabitCreationRequest(
                        "Привычка 1",
                        null,
                        false,
                        null,
                        FrequencyType.WEEKLY_X_TIMES,
                        null,
                        5,
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

        // Юзер создает отчет на сегодня
        reportHelper.createReport(
                token,
                new ReportCreationRequest(
                        existingHabitId,
                        LocalDate.now(),
                        null
                )
        );

        // Юзер получает отчет о привычке за сегодня (проверяем, что он есть)
        ReportFullInfoResponse reportFullInfoResponse1 = habitHelper.getReportAtDay(token, existingHabitId, TODAY_DATE_STR);
        assertThat(reportFullInfoResponse1.getReportId()).isNotNull();
        assertThat(reportFullInfoResponse1.isCompleted()).isTrue();
        assertThat(reportFullInfoResponse1.getCompletionTime()).isNotNull();
        assertThat(reportFullInfoResponse1.getPhotoUrl()).isNull();

        Long existingReportId = reportFullInfoResponse1.getReportId();

        // Юзер смотрит инфу по отчетам о созданной привычке (проверяем, что там есть выполнение)
        HabitReportsInfoResponse habitReportsInfoResponse1 = habitHelper.getReportsInfo(token, existingHabitId);
        assertThat(habitReportsInfoResponse1.getCompletionsInTotal()).isEqualTo(1);

        // Юзер удаляет отчет на сегодня
        reportHelper.deleteReport(token, existingReportId);

        // Юзер получает отчет о привычке за сегодня (проверяем, что его нет)
        ReportFullInfoResponse reportFullInfoResponse2 = habitHelper.getReportAtDay(token, existingHabitId, TODAY_DATE_STR);
        assertThat(reportFullInfoResponse2.getReportId()).isNull();
        assertThat(reportFullInfoResponse2.isCompleted()).isFalse();
        assertThat(reportFullInfoResponse2.getCompletionTime()).isNull();
        assertThat(reportFullInfoResponse2.getPhotoUrl()).isNull();

        // Юзер смотрит инфу по отчетам о созданной привычке (проверяем, что там нет выполнения)
        HabitReportsInfoResponse habitReportsInfoResponse2 = habitHelper.getReportsInfo(token, existingHabitId);
        assertThat(habitReportsInfoResponse2.getCompletionsInTotal()).isEqualTo(0);
    }

    @Test
    void testGettingHabitReportsInfo() throws InterruptedException {
        // Создается юзер с рандомным логином
        String userLogin = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin, "12345");
        String token = authHelper.login(userLogin, "12345").getToken();

        // Юзер создал привычку с WEEKLY_X_TIMES
        habitHelper.createHabit(
                token,
                new HabitCreationRequest(
                        "Привычка 1",
                        null,
                        false,
                        null,
                        FrequencyType.WEEKLY_X_TIMES,
                        null,
                        5,
                        null
                )
        );

        // Берем ID созданной привычки с WEEKLY_X_TIMES
        List<HabitShortInfoResponse> habitShortInfoResponses1 = habitHelper.getAllUserHabitsAtDay(token, TODAY_DATE_STR);
        Optional<HabitShortInfoResponse> existingHabitOptional1 = habitShortInfoResponses1.stream()
                .filter(h -> h.getName().equals("Привычка 1"))
                .findFirst();
        assertThat(existingHabitOptional1).isPresent();
        Long existingHabitId1 = existingHabitOptional1.get().getHabitId();

        // Юзер смотрит инфу по отчетам о созданной привычке с WEEKLY_X_TIMES
        HabitReportsInfoResponse habitReportsInfoResponse1 = habitHelper.getReportsInfo(token, existingHabitId1);
        assertThat(habitReportsInfoResponse1.getCompletionsInTotal()).isEqualTo(0);
        assertThat(habitReportsInfoResponse1.getCompletionsPercent()).isNull();
        assertThat(habitReportsInfoResponse1.getSerialDays()).isNull();
        assertThat(habitReportsInfoResponse1.getCompletionsInPeriod()).isEqualTo(0);
        assertThat(habitReportsInfoResponse1.getCompletionsPlannedInPeriod()).isEqualTo(5);
        assertThat(habitReportsInfoResponse1.getCompletedDays()).isEqualTo(List.of());
        assertThat(habitReportsInfoResponse1.getUncompletedDays()).isNull();

        // Юзер создает отчет на сегодня о созданной привычке с WEEKLY_X_TIMES
        reportHelper.createReport(
                token,
                new ReportCreationRequest(
                        existingHabitId1,
                        LocalDate.now(),
                        null
                )
        );

        // Юзер смотрит инфу по отчетам о созданной привычке с WEEKLY_X_TIMES
        HabitReportsInfoResponse habitReportsInfoResponse2 = habitHelper.getReportsInfo(token, existingHabitId1);
        assertThat(habitReportsInfoResponse2.getCompletionsInTotal()).isEqualTo(1);
        assertThat(habitReportsInfoResponse2.getCompletionsPercent()).isNull();
        assertThat(habitReportsInfoResponse2.getSerialDays()).isNull();
        assertThat(habitReportsInfoResponse2.getCompletionsInPeriod()).isEqualTo(1);
        assertThat(habitReportsInfoResponse2.getCompletionsPlannedInPeriod()).isEqualTo(5);
        assertThat(habitReportsInfoResponse2.getCompletedDays()).isEqualTo(List.of(LocalDate.now()));
        assertThat(habitReportsInfoResponse2.getUncompletedDays()).isNull();

        // Юзер создал привычку с WEEKLY_ON_DAYS, которая выполняется только в сегодняшний день недели
        habitHelper.createHabit(
                token,
                new HabitCreationRequest(
                        "Привычка 2",
                        null,
                        false,
                        null,
                        FrequencyType.WEEKLY_ON_DAYS,
                        Set.of(LocalDate.now().getDayOfWeek()),
                        null,
                        null
                )
        );

        // Берем ID созданной привычки с WEEKLY_X_TIMES
        List<HabitShortInfoResponse> habitShortInfoResponses2 = habitHelper.getAllUserHabitsAtDay(token, TODAY_DATE_STR);
        Optional<HabitShortInfoResponse> existingHabitOptional2 = habitShortInfoResponses2.stream()
                .filter(h -> h.getName().equals("Привычка 2"))
                .findFirst();
        assertThat(existingHabitOptional2).isPresent();
        Long existingHabitId2 = existingHabitOptional2.get().getHabitId();

        // Юзер смотрит инфу по отчетам о созданной привычке с WEEKLY_ON_DAYS
        HabitReportsInfoResponse habitReportsInfoResponse3 = habitHelper.getReportsInfo(token, existingHabitId2);
        assertThat(habitReportsInfoResponse3.getCompletionsInTotal()).isEqualTo(0);
        assertThat(habitReportsInfoResponse3.getCompletionsPercent()).isEqualTo(0);
        assertThat(habitReportsInfoResponse3.getSerialDays()).isNull();
        assertThat(habitReportsInfoResponse3.getCompletionsInPeriod()).isNull();
        assertThat(habitReportsInfoResponse3.getCompletionsPlannedInPeriod()).isNull();
        assertThat(habitReportsInfoResponse3.getCompletedDays()).isEqualTo(List.of());
        assertThat(habitReportsInfoResponse3.getUncompletedDays()).isEqualTo(List.of(LocalDate.now()));

        // Юзер создает отчет на сегодня о созданной привычке с WEEKLY_ON_DAYS
        reportHelper.createReport(
                token,
                new ReportCreationRequest(
                        existingHabitId2,
                        LocalDate.now(),
                        null
                )
        );

        // Юзер смотрит инфу по отчетам о созданной привычке с WEEKLY_ON_DAYS
        HabitReportsInfoResponse habitReportsInfoResponse4 = habitHelper.getReportsInfo(token, existingHabitId2);
        assertThat(habitReportsInfoResponse4.getCompletionsInTotal()).isEqualTo(1);
        assertThat(habitReportsInfoResponse4.getCompletionsPercent()).isEqualTo(100);
        assertThat(habitReportsInfoResponse4.getSerialDays()).isEqualTo(1);
        assertThat(habitReportsInfoResponse4.getCompletionsInPeriod()).isNull();
        assertThat(habitReportsInfoResponse4.getCompletionsPlannedInPeriod()).isNull();
        assertThat(habitReportsInfoResponse4.getCompletedDays()).isEqualTo(List.of(LocalDate.now()));
        assertThat(habitReportsInfoResponse4.getUncompletedDays()).isEqualTo(List.of());
    }

}
