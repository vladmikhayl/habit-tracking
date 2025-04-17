package com.vladmikhayl.report.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vladmikhayl.report.FeignClientTestConfig;
import com.vladmikhayl.report.entity.Report;
import com.vladmikhayl.report.repository.ReportRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static com.vladmikhayl.report.entity.FrequencyType.WEEKLY_ON_DAYS;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test") // чтобы CommandLineRunner в коде Application не выполнялся
@TestPropertySource(properties = {
        // чтобы Спринг не пытался использовать конфиг-сервер и Эврику
        "spring.config.location=classpath:/application-test.yml",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
@Transactional // чтобы после каждого теста все изменения, сделанные в БД, откатывались обратно
@Import(FeignClientTestConfig.class) // импортируем конфиг, где мы создали замоканный бин Feign-клиента
@AutoConfigureMockMvc
public class InternalReportControllerIntegrationTest {

    // При тестировании метода getReportsInfo() предполагается, что сегодня 7 апреля 2025
    // Все тесты написаны исходя их этого предположения. Если поменять здесь эту дату, то тесты могут не работать
    private static final LocalDate TODAY_DATE = LocalDate.of(2025, 4, 7);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        // Создаем бин для времени с TODAY_DATE и указываем, что именно он должен использоваться для времени
        // (так как у Спринга будет 2 бина: этот и из AppConfig)
        public Clock fixedClock() {
            return Clock.fixed(
                    TODAY_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    ZoneId.systemDefault()
            );
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportRepository reportRepository;

    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        // Явным образом получаем контейнер Postgres (если он еще не создавался, то в этот момент создастся его синглтон)
        TestPostgresContainer.getInstance();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Задаем настройки для БД, используя синглтон контейнера Postgres
        TestPostgresContainer container = TestPostgresContainer.getInstance();
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canGetReportAtDayWithPhotoWhenReportIsPresent() throws Exception {
        Long userId = 2L;
        Long habitId = 10L;

        Report existingReport = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl("https://photo-url.com/")
                .build();

        reportRepository.save(existingReport);

        mockMvc.perform(get("/internal/reports/get-report/of-habit/10/at-day/2025-03-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completionTime").exists())
                .andExpect(jsonPath("$.photoUrl").value("https://photo-url.com/"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canGetReportAtDayWithoutPhotoWhenReportIsPresent() throws Exception {
        Long userId = 2L;
        Long habitId = 10L;

        Report existingReport = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl(null)
                .build();

        reportRepository.save(existingReport);

        mockMvc.perform(get("/internal/reports/get-report/of-habit/10/at-day/2025-03-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completionTime").exists())
                .andExpect(jsonPath("$.photoUrl").isEmpty());
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canGetReportAtDayWhenReportIsNotPresent() throws Exception {
        mockMvc.perform(get("/internal/reports/get-report/of-habit/10/at-day/2025-03-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.completionTime").isEmpty())
                .andExpect(jsonPath("$.photoUrl").isEmpty());
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canCheckIsCompletedWhenReportIsPresent() throws Exception {
        Long userId = 2L;
        Long habitId = 10L;

        Report existingReport = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl("https://photo-url.com/")
                .build();

        reportRepository.save(existingReport);

        mockMvc.perform(get("/internal/reports/10/is-completed/at-day/2025-03-28"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canCheckIsCompletedWhenReportIsPresentForAnotherDay() throws Exception {
        Long userId = 2L;
        Long habitId = 10L;

        Report existingReport = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 27))
                .photoUrl("https://photo-url.com/")
                .build();

        reportRepository.save(existingReport);

        mockMvc.perform(get("/internal/reports/10/is-completed/at-day/2025-03-28"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canCheckIsCompletedWhenReportIsNotPresentAtAll() throws Exception {
        mockMvc.perform(get("/internal/reports/10/is-completed/at-day/2025-03-28"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canCountCompletionsInWeekPeriod() throws Exception {
        Long userId = 2L;
        Long habitId = 10L;

        // Создаем 3 отчета на искомой неделе для искомой привычки
        for (int i = 0; i < 3; i++) {
            Report existingReport = Report.builder()
                    .userId(userId)
                    .habitId(habitId)
                    .date(LocalDate.of(2025, 3, 24 + i))
                    .photoUrl(null)
                    .build();
            reportRepository.save(existingReport);
        }

        // Создаем 1 отчет на другой неделе для искомой привычки
        Report existingReport = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 23))
                .photoUrl(null)
                .build();
        reportRepository.save(existingReport);


        mockMvc.perform(get("/internal/reports/10/completion-count/WEEK/at/2025-03-29"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canCountCompletionsInWeekPeriodWhenAmountIsZero() throws Exception {
        mockMvc.perform(get("/internal/reports/10/completion-count/WEEK/at/2025-03-29"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canCountCompletionsInMonthPeriod() throws Exception {
        Long userId = 2L;
        Long habitId = 10L;

        // Создаем 5 отчетов в искомом месяце для искомой привычки
        for (int i = 0; i < 5; i++) {
            Report existingReport = Report.builder()
                    .userId(userId)
                    .habitId(habitId)
                    .date(LocalDate.of(2025, 2, 10 + i * 2))
                    .photoUrl(null)
                    .build();
            reportRepository.save(existingReport);
        }

        // Создаем 3 отчета в другом месяце для искомой привычки
        for (int i = 0; i < 3; i++) {
            Report existingReport = Report.builder()
                    .userId(userId)
                    .habitId(habitId)
                    .date(LocalDate.of(2025, 3, i + 1))
                    .photoUrl(null)
                    .build();
            reportRepository.save(existingReport);
        }

        // Создаем 1 отчет в искомом месяце, но для другой привычки
        Report existingReport = Report.builder()
                .userId(userId)
                .habitId(9L)
                .date(LocalDate.of(2025, 2, 23))
                .photoUrl(null)
                .build();
        reportRepository.save(existingReport);


        mockMvc.perform(get("/internal/reports/10/completion-count/MONTH/at/2025-02-01"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canCountCompletionsInMonthPeriodWhenAmountIsZero() throws Exception {
        Long userId = 2L;
        Long habitId = 10L;

        // Создаем 3 отчета в другом месяце для искомой привычки
        for (int i = 0; i < 3; i++) {
            Report existingReport = Report.builder()
                    .userId(userId)
                    .habitId(habitId)
                    .date(LocalDate.of(2025, 1, i + 1))
                    .photoUrl(null)
                    .build();
            reportRepository.save(existingReport);
        }

        mockMvc.perform(get("/internal/reports/10/completion-count/MONTH/at/2025-03-01"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test // WEEKLY ON DAYS во все дни, создана сегодня, не выполнена ни разу
    void testReportsInfoForWeeklyOnDaysEveryDayThatCreatedTodayWithZeroCompletions() throws Exception {
        mockMvc.perform(get("/internal/reports/10/reports-info")
                        .param("frequencyType", "WEEKLY_ON_DAYS")
                        .param("daysOfWeek", "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY")
                        .param("createdAt", TODAY_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionsInTotal").value(0))
                .andExpect(jsonPath("$.completionsPercent").value(0))
                .andExpect(jsonPath("$.serialDays").doesNotExist())
                .andExpect(jsonPath("$.completionsInPeriod").doesNotExist())
                .andExpect(jsonPath("$.completionsPlannedInPeriod").doesNotExist())
                .andExpect(jsonPath("$.completedDays").isArray())
                .andExpect(jsonPath("$.completedDays", containsInAnyOrder()))
                .andExpect(jsonPath("$.uncompletedDays").isArray())
                .andExpect(jsonPath("$.uncompletedDays", containsInAnyOrder(TODAY_DATE.toString())));
    }

    @Test // WEEKLY ON DAYS в сегодняшний и вчерашний день, создана вчера, выполнена сегодня
    void testReportsInfoForWeeklyOnDaysOnTodayAndYesterdayThatCreatedYesterdayWithOneCompletion() throws Exception {
        Long habitId = 10L;

        Report existingReport = Report.builder()
                .userId(50L)
                .habitId(habitId)
                .date(TODAY_DATE)
                .build();
        reportRepository.save(existingReport);

        mockMvc.perform(get("/internal/reports/10/reports-info")
                        .param("frequencyType", "WEEKLY_ON_DAYS")
                        .param("daysOfWeek", "MONDAY,SUNDAY")
                        .param("createdAt", TODAY_DATE.minusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionsInTotal").value(1))
                .andExpect(jsonPath("$.completionsPercent").value(50))
                .andExpect(jsonPath("$.serialDays").value(1))
                .andExpect(jsonPath("$.completionsInPeriod").doesNotExist())
                .andExpect(jsonPath("$.completionsPlannedInPeriod").doesNotExist())
                .andExpect(jsonPath("$.completedDays").isArray())
                .andExpect(jsonPath("$.completedDays", containsInAnyOrder(
                        TODAY_DATE.toString()
                )))
                .andExpect(jsonPath("$.uncompletedDays").isArray())
                .andExpect(jsonPath("$.uncompletedDays", containsInAnyOrder(
                        TODAY_DATE.minusDays(1).toString()
                )));
    }

    @Test
        // WEEKLY ON DAYS только в сегодняшний день, создана месяц назад, выполнена 3 раза
        // (из них 2 в эту серию, причем сегодня еще не выполнена)
    void testReportsInfoForWeeklyOnDaysOnTodayThatCreatedMonthAgoWithThreeCompletions() throws Exception {
        Long habitId = 10L;

        Report existingReport1 = Report.builder()
                .userId(50L)
                .habitId(habitId)
                .date(TODAY_DATE.minusDays(7))
                .build();
        Report existingReport2 = Report.builder()
                .userId(50L)
                .habitId(habitId)
                .date(TODAY_DATE.minusDays(14))
                .build();
        Report existingReport3 = Report.builder()
                .userId(50L)
                .habitId(habitId)
                .date(TODAY_DATE.minusDays(28))
                .build();
        reportRepository.save(existingReport1);
        reportRepository.save(existingReport2);
        reportRepository.save(existingReport3);

        // Отчет о другой привычке (он не должен участвовать в статистике)
        Report existingReport4 = Report.builder()
                .userId(50L)
                .habitId(habitId + 1)
                .date(TODAY_DATE)
                .build();
        reportRepository.save(existingReport4);

        mockMvc.perform(get("/internal/reports/10/reports-info")
                        .param("frequencyType", "WEEKLY_ON_DAYS")
                        .param("daysOfWeek", "MONDAY")
                        .param("createdAt", TODAY_DATE.minusDays(30).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionsInTotal").value(3))
                .andExpect(jsonPath("$.completionsPercent").value(60))
                .andExpect(jsonPath("$.serialDays").value(2))
                .andExpect(jsonPath("$.completionsInPeriod").doesNotExist())
                .andExpect(jsonPath("$.completionsPlannedInPeriod").doesNotExist())
                .andExpect(jsonPath("$.completedDays").isArray())
                .andExpect(jsonPath("$.completedDays", containsInAnyOrder(
                        TODAY_DATE.minusDays(7).toString(),
                        TODAY_DATE.minusDays(14).toString(),
                        TODAY_DATE.minusDays(28).toString()
                )))
                .andExpect(jsonPath("$.uncompletedDays").isArray())
                .andExpect(jsonPath("$.uncompletedDays", containsInAnyOrder(
                        TODAY_DATE.toString(),
                        TODAY_DATE.minusDays(21).toString()
                )));
    }

    @Test // WEEKLY ON DAYS во все дни, создана полгода назад, выполнена всегда кроме вчера и сегодня
    void testReportsInfoForWeeklyOnDaysEveryDayThatCreatedSixMonthsAgoWithAllCompletionsExceptTwo() throws Exception {
        Long habitId = 10L;

        for (int i = 2; i <= 180; i++) {
            Report existingReport = Report.builder()
                    .userId(50L)
                    .habitId(habitId)
                    .date(TODAY_DATE.minusDays(i))
                    .build();
            reportRepository.save(existingReport);
        }

        List<String> reportDates = IntStream.rangeClosed(2, 180)
                .mapToObj(TODAY_DATE::minusDays)
                .map(LocalDate::toString)
                .toList();

        mockMvc.perform(get("/internal/reports/10/reports-info")
                        .param("frequencyType", "WEEKLY_ON_DAYS")
                        .param("daysOfWeek", "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY")
                        .param("createdAt", TODAY_DATE.minusDays(180).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionsInTotal").value(179))
                .andExpect(jsonPath("$.completionsPercent").value(98))
                .andExpect(jsonPath("$.serialDays").value(0))
                .andExpect(jsonPath("$.completionsInPeriod").doesNotExist())
                .andExpect(jsonPath("$.completionsPlannedInPeriod").doesNotExist())
                .andExpect(jsonPath("$.completedDays").isArray())
                .andExpect(jsonPath("$.completedDays", containsInAnyOrder(reportDates.toArray(String[]::new))))
                .andExpect(jsonPath("$.uncompletedDays").isArray())
                .andExpect(jsonPath("$.uncompletedDays", containsInAnyOrder(
                        TODAY_DATE.toString(),
                        TODAY_DATE.minusDays(1).toString()
                )));
    }

    @Test // WEEKLY X TIMES 1 раз, создана сегодня, не выполнена ни разу
    void testReportsInfoForWeeklyXTimesOneTimeThatCreatedTodayWithZeroCompletions() throws Exception {
        mockMvc.perform(get("/internal/reports/10/reports-info")
                        .param("frequencyType", "WEEKLY_X_TIMES")
                        .param("timesPerWeek", "1")
                        .param("createdAt", TODAY_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionsInTotal").value(0))
                .andExpect(jsonPath("$.completionsPercent").doesNotExist())
                .andExpect(jsonPath("$.serialDays").doesNotExist())
                .andExpect(jsonPath("$.completionsInPeriod").value(0))
                .andExpect(jsonPath("$.completionsPlannedInPeriod").value(1))
                .andExpect(jsonPath("$.completedDays").isArray())
                .andExpect(jsonPath("$.completedDays", containsInAnyOrder()))
                .andExpect(jsonPath("$.uncompletedDays").doesNotExist());
    }

    @Test // WEEKLY X TIMES 5 раз, создана месяц назад, выполнена 3 раза (из них на этой неделе 1)
    void testReportsInfoForWeeklyXTimesFiveTimesThatCreatedMonthAgoWithThreeCompletions() throws Exception {
        Long habitId = 10L;

        Report existingReport1 = Report.builder()
                .userId(50L)
                .habitId(habitId)
                .date(TODAY_DATE)
                .build();
        Report existingReport2 = Report.builder()
                .userId(50L)
                .habitId(habitId)
                .date(TODAY_DATE.minusDays(8))
                .build();
        Report existingReport3 = Report.builder()
                .userId(50L)
                .habitId(habitId)
                .date(TODAY_DATE.minusDays(9))
                .build();
        reportRepository.save(existingReport1);
        reportRepository.save(existingReport2);
        reportRepository.save(existingReport3);

        // Отчет о другой привычке (он не должен участвовать в статистике)
        Report existingReport4 = Report.builder()
                .userId(50L)
                .habitId(habitId + 1)
                .date(TODAY_DATE.minusDays(10))
                .build();
        reportRepository.save(existingReport4);

        mockMvc.perform(get("/internal/reports/10/reports-info")
                        .param("frequencyType", "WEEKLY_X_TIMES")
                        .param("timesPerWeek", "5")
                        .param("createdAt", TODAY_DATE.minusDays(30).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionsInTotal").value(3))
                .andExpect(jsonPath("$.completionsPercent").doesNotExist())
                .andExpect(jsonPath("$.serialDays").doesNotExist())
                .andExpect(jsonPath("$.completionsInPeriod").value(1))
                .andExpect(jsonPath("$.completionsPlannedInPeriod").value(5))
                .andExpect(jsonPath("$.completedDays").isArray())
                .andExpect(jsonPath("$.completedDays", containsInAnyOrder(
                        TODAY_DATE.toString(),
                        TODAY_DATE.minusDays(8).toString(),
                        TODAY_DATE.minusDays(9).toString()
                )))
                .andExpect(jsonPath("$.uncompletedDays").doesNotExist());
    }

    @Test // WEEKLY X TIMES 7 раз, создана полгода назад, выполнена всегда кроме вчера и сегодня
    void testReportsInfoForWeeklyXTimesSevenTimesThatCreatedHalfYearAgoWithAllCompletionsExceptTwo() throws Exception {
        Long habitId = 10L;

        for (int i = 2; i <= 180; i++) {
            Report existingReport = Report.builder()
                    .userId(50L)
                    .habitId(habitId)
                    .date(TODAY_DATE.minusDays(i))
                    .build();
            reportRepository.save(existingReport);
        }

        List<String> reportDates = IntStream.rangeClosed(2, 180)
                .mapToObj(TODAY_DATE::minusDays)
                .map(LocalDate::toString)
                .toList();

        mockMvc.perform(get("/internal/reports/10/reports-info")
                        .param("frequencyType", "WEEKLY_X_TIMES")
                        .param("timesPerWeek", "7")
                        .param("createdAt", TODAY_DATE.minusDays(180).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionsInTotal").value(179))
                .andExpect(jsonPath("$.completionsPercent").doesNotExist())
                .andExpect(jsonPath("$.serialDays").doesNotExist())
                .andExpect(jsonPath("$.completionsInPeriod").value(0))
                .andExpect(jsonPath("$.completionsPlannedInPeriod").value(7))
                .andExpect(jsonPath("$.completedDays").isArray())
                .andExpect(jsonPath("$.completedDays", containsInAnyOrder(reportDates.toArray(String[]::new))))
                .andExpect(jsonPath("$.uncompletedDays").doesNotExist());
    }

    @Test // MONTHLY X TIMES 1 раз, создана сегодня, не выполнена ни разу
    void testReportsInfoForMonthlyXTimesOneTimeThatCreatedTodayWithZeroCompletions() throws Exception {
        mockMvc.perform(get("/internal/reports/10/reports-info")
                        .param("frequencyType", "MONTHLY_X_TIMES")
                        .param("timesPerMonth", "1")
                        .param("createdAt", TODAY_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionsInTotal").value(0))
                .andExpect(jsonPath("$.completionsPercent").doesNotExist())
                .andExpect(jsonPath("$.serialDays").doesNotExist())
                .andExpect(jsonPath("$.completionsInPeriod").value(0))
                .andExpect(jsonPath("$.completionsPlannedInPeriod").value(1))
                .andExpect(jsonPath("$.completedDays").isArray())
                .andExpect(jsonPath("$.completedDays", containsInAnyOrder()))
                .andExpect(jsonPath("$.uncompletedDays").doesNotExist());
    }

    @Test // MONTHLY X TIMES 5 раз, создана месяц назад, выполнена 3 раза (и все в этом месяце)
    void testReportsInfoForMonthlyXTimesFiveTimesThatCreatedMonthAgoWithThreeCompletions() throws Exception {
        Long habitId = 10L;

        Report existingReport1 = Report.builder()
                .userId(50L)
                .habitId(habitId)
                .date(TODAY_DATE)
                .build();
        Report existingReport2 = Report.builder()
                .userId(50L)
                .habitId(habitId)
                .date(TODAY_DATE.minusDays(2))
                .build();
        Report existingReport3 = Report.builder()
                .userId(50L)
                .habitId(habitId)
                .date(TODAY_DATE.minusDays(6))
                .build();
        reportRepository.save(existingReport1);
        reportRepository.save(existingReport2);
        reportRepository.save(existingReport3);

        // Отчет о другой привычке (он не должен участвовать в статистике)
        Report existingReport4 = Report.builder()
                .userId(50L)
                .habitId(habitId + 1)
                .date(TODAY_DATE.minusDays(5))
                .build();
        reportRepository.save(existingReport4);

        mockMvc.perform(get("/internal/reports/10/reports-info")
                        .param("frequencyType", "MONTHLY_X_TIMES")
                        .param("timesPerMonth", "5")
                        .param("createdAt", TODAY_DATE.minusDays(30).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionsInTotal").value(3))
                .andExpect(jsonPath("$.completionsPercent").doesNotExist())
                .andExpect(jsonPath("$.serialDays").doesNotExist())
                .andExpect(jsonPath("$.completionsInPeriod").value(3))
                .andExpect(jsonPath("$.completionsPlannedInPeriod").value(5))
                .andExpect(jsonPath("$.completedDays").isArray())
                .andExpect(jsonPath("$.completedDays", containsInAnyOrder(
                        TODAY_DATE.toString(),
                        TODAY_DATE.minusDays(2).toString(),
                        TODAY_DATE.minusDays(6).toString()
                )))
                .andExpect(jsonPath("$.uncompletedDays").doesNotExist());
    }

    @Test // MONTHLY X TIMES 31 раз, создана полгода назад, выполнена всегда кроме вчера и сегодня
    void testReportsInfoForMonthlyXTimes31TimesThatCreatedHalfYearAgoWithAllCompletionsExceptTwo() throws Exception {
        Long habitId = 10L;

        for (int i = 2; i <= 180; i++) {
            Report existingReport = Report.builder()
                    .userId(50L)
                    .habitId(habitId)
                    .date(TODAY_DATE.minusDays(i))
                    .build();
            reportRepository.save(existingReport);
        }

        List<String> reportDates = IntStream.rangeClosed(2, 180)
                .mapToObj(TODAY_DATE::minusDays)
                .map(LocalDate::toString)
                .toList();

        mockMvc.perform(get("/internal/reports/10/reports-info")
                        .param("frequencyType", "MONTHLY_X_TIMES")
                        .param("timesPerMonth", "31")
                        .param("createdAt", TODAY_DATE.minusDays(180).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionsInTotal").value(179))
                .andExpect(jsonPath("$.completionsPercent").doesNotExist())
                .andExpect(jsonPath("$.serialDays").doesNotExist())
                .andExpect(jsonPath("$.completionsInPeriod").value(5))
                .andExpect(jsonPath("$.completionsPlannedInPeriod").value(31))
                .andExpect(jsonPath("$.completedDays").isArray())
                .andExpect(jsonPath("$.completedDays", containsInAnyOrder(reportDates.toArray(String[]::new))))
                .andExpect(jsonPath("$.uncompletedDays").doesNotExist());
    }

}
