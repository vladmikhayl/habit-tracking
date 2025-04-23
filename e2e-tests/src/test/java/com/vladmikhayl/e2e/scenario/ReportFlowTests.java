package com.vladmikhayl.e2e.scenario;

import com.vladmikhayl.e2e.BaseE2ETest;
import com.vladmikhayl.e2e.dto.habit.HabitCreationRequest;
import com.vladmikhayl.e2e.dto.habit.HabitShortInfoResponse;
import com.vladmikhayl.e2e.dto.habit.ReportFullInfoResponse;
import com.vladmikhayl.e2e.dto.report.ReportCreationRequest;
import com.vladmikhayl.e2e.dto.report.ReportPhotoEditingRequest;
import com.vladmikhayl.e2e.entity.FrequencyType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportFlowTests extends BaseE2ETest {

    @Test
    void testReportsCreating() throws InterruptedException {
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
        assertThat(reportFullInfoResponse2.isCompleted()).isTrue();
        assertThat(reportFullInfoResponse2.getCompletionTime()).isNotNull();
        assertThat(reportFullInfoResponse2.getPhotoUrl()).isNull();

        // Юзер получает отчет о привычке за завтра (проверяем, что его нет)
        ReportFullInfoResponse reportFullInfoResponse3 = habitHelper.getReportAtDay(token, existingHabitId, TOMORROW_DATE_STR);
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

}
