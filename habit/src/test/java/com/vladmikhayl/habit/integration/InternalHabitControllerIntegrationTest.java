package com.vladmikhayl.habit.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmikhayl.habit.FeignClientTestConfig;
import com.vladmikhayl.habit.entity.FrequencyType;
import com.vladmikhayl.habit.entity.HabitWithoutAutoCreationTime;
import com.vladmikhayl.habit.entity.HabitWithoutAutoCreationTimeRepository;
import com.vladmikhayl.habit.repository.HabitRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

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
@Import(FeignClientTestConfig.class) // импортируем конфиг, где мы создали замоканный бин ReportClient
@AutoConfigureMockMvc
public class InternalHabitControllerIntegrationTest {

    // При тестировании метода isCurrent() предполагается, что сегодня 9 апреля 2025
    // Все тесты написаны исходя их этого предположения. Если поменять здесь эту дату, то тесты могут не работать
    private static final LocalDate TODAY_DATE = LocalDate.of(2025, 4, 9);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private HabitWithoutAutoCreationTimeRepository habitWithoutAutoCreationTimeRepository;

    @Autowired
    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void setUp() {
        objectMapper = new ObjectMapper();

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
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testIsCurrentForWeeklyOnDaysWhenItIsLastDay() throws Exception {
        Long userId = 12L;
        String userIdStr = "12";

        HabitWithoutAutoCreationTime habit = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название")
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY))
                .durationDays(14)
                .createdAt(LocalDateTime.of(2025, 3, 27, 12, 30))
                .build();

        habitWithoutAutoCreationTimeRepository.save(habit);

        mockMvc.perform(get("/internal/habits/1/is-current")
                        .param("userId", userIdStr)
                        .param("date", TODAY_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testIsCurrentForWeeklyOnDaysWhenItIsExpired() throws Exception {
        Long userId = 12L;
        String userIdStr = "12";

        HabitWithoutAutoCreationTime habit = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название")
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY))
                .durationDays(14)
                .createdAt(LocalDateTime.of(2025, 3, 26, 23, 59))
                .build();

        habitWithoutAutoCreationTimeRepository.save(habit);

        mockMvc.perform(get("/internal/habits/1/is-current")
                        .param("userId", userIdStr)
                        .param("date", TODAY_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testIsCurrentForWeeklyOnDaysWhenItIsWrongWeekDay() throws Exception {
        Long userId = 12L;
        String userIdStr = "12";

        HabitWithoutAutoCreationTime habit = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название")
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.THURSDAY))
                .durationDays(null)
                .createdAt(LocalDateTime.of(2025, 4, 2, 14, 0))
                .build();

        habitWithoutAutoCreationTimeRepository.save(habit);

        mockMvc.perform(get("/internal/habits/1/is-current")
                        .param("userId", userIdStr)
                        .param("date", TODAY_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testIsCurrentForMonthlyXTimesWhenItBelongsToAnotherUser() throws Exception {
        String userIdStr = "12";

        HabitWithoutAutoCreationTime habit = HabitWithoutAutoCreationTime.builder()
                .userId(11L)
                .name("Название")
                .frequencyType(FrequencyType.MONTHLY_X_TIMES)
                .timesPerMonth(10)
                .durationDays(null)
                .createdAt(LocalDateTime.of(2025, 4, 2, 14, 0))
                .build();

        habitWithoutAutoCreationTimeRepository.save(habit);

        mockMvc.perform(get("/internal/habits/1/is-current")
                        .param("userId", userIdStr)
                        .param("date", TODAY_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testIsCurrentForMonthlyXTimesWhenDateIsCorrect() throws Exception {
        Long userId = 12L;
        String userIdStr = "12";

        HabitWithoutAutoCreationTime habit = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название")
                .frequencyType(FrequencyType.MONTHLY_X_TIMES)
                .timesPerMonth(5)
                .durationDays(1)
                .createdAt(LocalDateTime.of(2025, 4, 9, 22, 0))
                .build();

        habitWithoutAutoCreationTimeRepository.save(habit);

        mockMvc.perform(get("/internal/habits/1/is-current")
                        .param("userId", userIdStr)
                        .param("date", TODAY_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testIsCurrentForMonthlyXTimesWhenCreationDateIsAfterDate() throws Exception {
        Long userId = 12L;
        String userIdStr = "12";

        HabitWithoutAutoCreationTime habit = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название")
                .frequencyType(FrequencyType.MONTHLY_X_TIMES)
                .timesPerMonth(5)
                .durationDays(null)
                .createdAt(LocalDateTime.of(2025, 4, 9, 22, 0))
                .build();

        habitWithoutAutoCreationTimeRepository.save(habit);

        mockMvc.perform(get("/internal/habits/1/is-current")
                        .param("userId", userIdStr)
                        .param("date", TODAY_DATE.minusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

}
