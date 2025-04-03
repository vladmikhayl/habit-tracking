package com.vladmikhayl.report.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vladmikhayl.report.entity.Report;
import com.vladmikhayl.report.repository.ReportRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
@AutoConfigureMockMvc
public class InternalReportControllerIntegrationTest {

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
                    .date(LocalDate.of(2025, 2, 10 + i*2))
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

}
