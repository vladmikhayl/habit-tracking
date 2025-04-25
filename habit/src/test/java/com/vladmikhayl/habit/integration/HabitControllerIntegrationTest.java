package com.vladmikhayl.habit.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmikhayl.habit.FeignClientTestConfig;
import com.vladmikhayl.habit.dto.request.HabitCreationRequest;
import com.vladmikhayl.habit.dto.request.HabitEditingRequest;
import com.vladmikhayl.habit.dto.response.HabitReportsInfoResponse;
import com.vladmikhayl.habit.dto.response.HabitShortInfoResponse;
import com.vladmikhayl.habit.dto.response.ReportFullInfoResponse;
import com.vladmikhayl.habit.dto.response.SubscribedHabitShortInfoResponse;
import com.vladmikhayl.habit.entity.Period;
import com.vladmikhayl.habit.entity.*;
import com.vladmikhayl.habit.repository.HabitRepository;
import com.vladmikhayl.habit.repository.SubscriptionCacheRepository;
import com.vladmikhayl.habit.service.feign.ReportClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.vladmikhayl.habit.entity.FrequencyType.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
// чтобы создалась встроенная Кафка, которая не будет отправлять сообщения на реальные микросервисы
@EmbeddedKafka(partitions = 1, topics = {"habit-created", "habit-deleted"})
@Import(FeignClientTestConfig.class) // импортируем конфиг, где мы создали замоканный бин Feign-клиента
@AutoConfigureMockMvc
public class HabitControllerIntegrationTest {

    // При тестировании методов getGeneralInfo() и getAllUserHabitsAtDay() предполагается, что сегодня 12 апреля 2025
    // Все тесты написаны исходя их этого предположения. Если поменять здесь эту дату, то тесты могут не работать
    private static final LocalDate TODAY_DATE = LocalDate.of(2025, 4, 12);

    @Value("${internal.token}")
    private String testInternalToken;

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
    private HabitRepository habitRepository;

    @Autowired
    private ReportClient reportClient;

    @Autowired
    private SubscriptionCacheRepository subscriptionCacheRepository;

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
    void canCreateCorrectHabitWithWeeklyOnDaysFrequency() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Название")
                .description("Описание")
                .isPhotoAllowed(true)
                .isHarmful(true)
                .durationDays(30)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        String userIdStr = "1";
        Long userId = 1L;

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isCreated());

        Optional<Habit> habit = habitRepository.findByName("Название");
        assertThat(habit.isPresent()).isTrue();

        Habit expected = Habit.builder()
                .userId(userId)
                .name("Название")
                .description("Описание")
                .isPhotoAllowed(true)
                .isHarmful(true)
                .durationDays(30)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        assertThat(habit.get())
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt")
                .isEqualTo(expected);
    }

    @Test
    void canCreateCorrectHabitWithWeeklyXTimesFrequency() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Название")
                .description(null)
                .isPhotoAllowed(false)
                .isHarmful(false)
                .durationDays(null)
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(2)
                .timesPerMonth(null)
                .build();

        String userIdStr = "1";
        Long userId = 1L;

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isCreated());

        Optional<Habit> habit = habitRepository.findByName("Название");
        assertThat(habit.isPresent()).isTrue();

        Habit expected = Habit.builder()
                .userId(userId)
                .name("Название")
                .description(null)
                .isPhotoAllowed(false)
                .isHarmful(false)
                .durationDays(null)
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(2)
                .timesPerMonth(null)
                .build();

        assertThat(habit.get())
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt")
                .isEqualTo(expected);
    }

    @Test
    void canCreateCorrectHabitWithMonthlyXTimesFrequency() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Название")
                .description(null)
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(5)
                .frequencyType(FrequencyType.MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(15)
                .build();

        String userIdStr = "3";
        Long userId = 3L;

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isCreated());

        Optional<Habit> habit = habitRepository.findByName("Название");
        assertThat(habit.isPresent()).isTrue();

        Habit expected = Habit.builder()
                .userId(userId)
                .name("Название")
                .description(null)
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(5)
                .frequencyType(FrequencyType.MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(15)
                .build();

        assertThat(habit.get())
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt")
                .isEqualTo(expected);
    }

    @Test
    void failCreateHabitWhenUserAlreadyHasHabitWithSameName() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Название")
                .description(null)
                .isPhotoAllowed(false)
                .isHarmful(false)
                .durationDays(5)
                .frequencyType(FrequencyType.MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(15)
                .build();

        String userIdStr = "1";
        Long userId = 1L;

        Habit existingHabit = Habit.builder()
                .userId(userId)
                .name("Название")
                .description(null)
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(5)
                .frequencyType(FrequencyType.MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(15)
                .build();

        habitRepository.save(existingHabit);

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("This user already has a habit with that name"));

        long habitsCount = habitRepository.count();
        assertThat(habitsCount).isEqualTo(1);
    }

    @Test
    void failCreateHabitWithWrongHarmfulSetting() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Название")
                .description(null)
                .isPhotoAllowed(false)
                .isHarmful(true)
                .durationDays(5)
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(1)
                .timesPerMonth(null)
                .build();

        String userIdStr = "1";

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("A habit with this FrequencyType cannot be harmful"));

        long habitsCount = habitRepository.count();
        assertThat(habitsCount).isEqualTo(0);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canEditHabitWithCorrectRequest() throws Exception {
        String userIdStr = "1";
        Long userId = 1L;

        Habit existingHabit = Habit.builder()
                .userId(userId)
                .name("Старое название")
                .description(null)
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(5)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        habitRepository.save(existingHabit);

        HabitEditingRequest request = HabitEditingRequest.builder()
//                .name("Новое название")
                .description("Описание")
                .isHarmful(true)
                .durationDays(30)
                .build();

        mockMvc.perform(put("/api/v1/habits/1/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk());

        Habit expected = Habit.builder()
                .id(1L)
                .userId(userId)
                .name("Старое название")
                .description("Описание")
                .isPhotoAllowed(true)
                .isHarmful(true)
                .durationDays(30)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        Optional<Habit> habit = habitRepository.findByName("Старое название");
        assertThat(habit.isPresent()).isTrue();

        assertThat(habit.get())
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(expected);

        long habitsCount = habitRepository.count();
        assertThat(habitsCount).isEqualTo(1);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canEditHabitWithDurationDaysEqualsToZero() throws Exception {
        String userIdStr = "1";
        Long userId = 1L;

        Habit existingHabit = Habit.builder()
                .userId(userId)
                .name("Старое название")
                .description(null)
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(5)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        habitRepository.save(existingHabit);

        HabitEditingRequest request = HabitEditingRequest.builder()
//                .name(null)
                .description(null)
                .isHarmful(null)
                .durationDays(0)
                .build();

        mockMvc.perform(put("/api/v1/habits/1/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk());

        Habit expected = Habit.builder()
                .id(1L)
                .userId(userId)
                .name("Старое название")
                .description(null)
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(null)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        Optional<Habit> habit = habitRepository.findByName("Старое название");
        assertThat(habit.isPresent()).isTrue();

        assertThat(habit.get())
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(expected);

        long habitsCount = habitRepository.count();
        assertThat(habitsCount).isEqualTo(1);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failEditHabitWhenUserDoesNotHaveThisHabit() throws Exception {
        String userIdStr = "1";

        Habit existingHabit = Habit.builder()
                .userId(2L)
                .name("Старое название")
                .description(null)
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(5)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        habitRepository.save(existingHabit);

        HabitEditingRequest request = HabitEditingRequest.builder()
//                .name("Новое название")
                .description("Описание")
                .isHarmful(true)
                .durationDays(30)
                .build();

        mockMvc.perform(put("/api/v1/habits/1/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This user doesn't have a habit with this id"));

        Optional<Habit> oldHabit = habitRepository.findByName("Старое название");
        assertThat(oldHabit.isPresent()).isTrue();

        assertThat(oldHabit.get())
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt")
                .isEqualTo(existingHabit);

        long habitsCount = habitRepository.count();
        assertThat(habitsCount).isEqualTo(1);
    }

//    @Test
//    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
//    void failEditHabitWhenUserAlreadyHasHabitWithSameName() throws Exception {
//        String userIdStr = "1";
//        Long userId = 1L;
//
//        Habit existingHabit1 = Habit.builder()
//                .userId(userId)
//                .name("Название 1")
//                .description(null)
//                .isPhotoAllowed(true)
//                .isHarmful(false)
//                .durationDays(5)
//                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
//                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
//                .timesPerWeek(null)
//                .timesPerMonth(null)
//                .build();
//
//        habitRepository.save(existingHabit1);
//
//        Habit existingHabit2 = Habit.builder()
//                .userId(userId)
//                .name("Название 2")
//                .description(null)
//                .isPhotoAllowed(true)
//                .isHarmful(false)
//                .durationDays(5)
//                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
//                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
//                .timesPerWeek(null)
//                .timesPerMonth(null)
//                .build();
//
//        habitRepository.save(existingHabit2);
//
//        HabitEditingRequest request = HabitEditingRequest.builder()
//                .name("Название 1")
//                .description(null)
//                .isHarmful(null)
//                .durationDays(null)
//                .build();
//
//        mockMvc.perform(put("/api/v1/habits/2/edit")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request))
//                        .header("X-User-Id", userIdStr))
//                .andExpect(status().isConflict())
//                .andExpect(jsonPath("$.error").value("This user already has a habit with that name"));
//
//        Optional<Habit> oldHabit = habitRepository.findByName("Название 2");
//        assertThat(oldHabit.isPresent()).isTrue();
//
//        assertThat(oldHabit.get())
//                .usingRecursiveComparison()
//                .ignoringFields("id", "createdAt")
//                .isEqualTo(existingHabit2);
//
//        long habitsCount = habitRepository.count();
//        assertThat(habitsCount).isEqualTo(2);
//    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failSetHarmfulTrueWithNotWeeklyOnDaysFrequency() throws Exception {
        String userIdStr = "1";
        Long userId = 1L;

        Habit existingHabit = Habit.builder()
                .userId(userId)
                .name("Название")
                .description(null)
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(5)
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(2)
                .timesPerMonth(null)
                .build();

        habitRepository.save(existingHabit);

        HabitEditingRequest request = HabitEditingRequest.builder()
//                .name(null)
                .description(null)
                .isHarmful(true)
                .durationDays(null)
                .build();

        mockMvc.perform(put("/api/v1/habits/1/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("A habit with this FrequencyType cannot be harmful"));

        Optional<Habit> oldHabit = habitRepository.findByName("Название");
        assertThat(oldHabit.isPresent()).isTrue();

        assertThat(oldHabit.get())
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt")
                .isEqualTo(existingHabit);

        long habitsCount = habitRepository.count();
        assertThat(habitsCount).isEqualTo(1);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canDeleteHabitWhenItBelongsToUser() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;

        Habit existingHabit1 = Habit.builder()
                .name("Название 1")
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY))
                .userId(userId)
                .build();
        habitRepository.save(existingHabit1);

        Habit existingHabit2 = Habit.builder()
                .name("Название 2")
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY))
                .userId(userId)
                .build();
        habitRepository.save(existingHabit2);

        assertThat(habitRepository.count()).isEqualTo(2);

        mockMvc.perform(delete("/api/v1/habits/1/delete")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk());

        assertThat(habitRepository.count()).isEqualTo(1);

        Optional<Habit> foundHabit = habitRepository.findById(2L);

        assertThat(foundHabit.isPresent()).isTrue();
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failDeleteHabitWhenItBelongsToAnotherUser() throws Exception {
        String userIdStr = "10";

        Habit existingHabit1 = Habit.builder()
                .name("Название 1")
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .timesPerWeek(5)
                .userId(11L)
                .build();
        habitRepository.save(existingHabit1);

        assertThat(habitRepository.count()).isEqualTo(1);

        mockMvc.perform(delete("/api/v1/habits/1/delete")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This user doesn't have a habit with this id"));

        assertThat(habitRepository.count()).isEqualTo(1);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failDeleteHabitWhenItDoesNotExist() throws Exception {
        String userIdStr = "10";

        assertThat(habitRepository.count()).isEqualTo(0);

        mockMvc.perform(delete("/api/v1/habits/1/delete")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This user doesn't have a habit with this id"));

        assertThat(habitRepository.count()).isEqualTo(0);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canGetReportsInfoForWeeklyOnDaysWhenUserIsCreator() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;

        Habit habit = Habit.builder()
                .userId(userId)
                .name("Название")
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .build();

        habitRepository.save(habit);

        Mockito.when(reportClient.getReportsInfo(
                eq(testInternalToken),
                eq(1L),
                eq(FrequencyType.WEEKLY_ON_DAYS),
                eq(Set.of(DayOfWeek.MONDAY)),
                eq(null),
                eq(null),
                any()
        )).thenReturn(ResponseEntity.ok(HabitReportsInfoResponse.builder()
                .completionsInTotal(1)
                .completionsPercent(100)
                .serialDays(1)
                .completedDays(List.of(LocalDate.of(2025, 4, 10)))
                .uncompletedDays(List.of())
                .build()));

        mockMvc.perform(get("/api/v1/habits/1/reports-info")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionsInTotal").value("1"))
                .andExpect(jsonPath("$.completionsPercent").value("100"))
                .andExpect(jsonPath("$.serialDays").value("1"))
                .andExpect(jsonPath("$.completionsInPeriod").doesNotExist())
                .andExpect(jsonPath("$.completionsPlannedInPeriod").doesNotExist())
                .andExpect(jsonPath("$.completedDays").isArray())
                .andExpect(jsonPath("$.uncompletedDays").isArray());
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canGetReportsInfoForMonthlyXTimesWhenUserIsSubscriber() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;

        // Привычку с ID=1 создал другой юзер
        Habit habit = Habit.builder()
                .userId(12L)
                .name("Название")
                .frequencyType(FrequencyType.MONTHLY_X_TIMES)
                .timesPerMonth(10)
                .build();

        habitRepository.save(habit);

        SubscriptionCache subscriptionCache = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(1L)
                        .subscriberId(userId)
                        .build())
                .creatorLogin("user12")
                .build();

        subscriptionCacheRepository.save(subscriptionCache);

        Mockito.when(reportClient.getReportsInfo(
                eq(testInternalToken),
                eq(1L),
                eq(FrequencyType.MONTHLY_X_TIMES),
                eq(null),
                eq(null),
                eq(10),
                any()
        )).thenReturn(ResponseEntity.ok(HabitReportsInfoResponse.builder()
                .completionsInTotal(1)
                .completionsInPeriod(1)
                .completionsPlannedInPeriod(10)
                .completedDays(List.of(LocalDate.of(2025, 4, 10)))
                .build()));

        mockMvc.perform(get("/api/v1/habits/1/reports-info")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionsInTotal").value("1"))
                .andExpect(jsonPath("$.completionsPercent").doesNotExist())
                .andExpect(jsonPath("$.serialDays").doesNotExist())
                .andExpect(jsonPath("$.completionsInPeriod").value("1"))
                .andExpect(jsonPath("$.completionsPlannedInPeriod").value("10"))
                .andExpect(jsonPath("$.completedDays").isArray())
                .andExpect(jsonPath("$.uncompletedDays").doesNotExist());
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failGetReportsInfoWhenUserIsNotCreatorAndIsNotSubscriber() throws Exception {
        String userIdStr = "10";

        // Привычку с ID=1 создал другой юзер
        Habit habit = Habit.builder()
                .userId(12L)
                .name("Название")
                .frequencyType(FrequencyType.MONTHLY_X_TIMES)
                .timesPerMonth(10)
                .build();

        habitRepository.save(habit);

        // На привычку с ID=1 подписан другой юзер
        SubscriptionCache subscriptionCache = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(1L)
                        .subscriberId(11L)
                        .build())
                .creatorLogin("user12")
                .build();

        subscriptionCacheRepository.save(subscriptionCache);

        mockMvc.perform(get("/api/v1/habits/1/reports-info")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This user doesn't have access to this habit"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failGetReportsInfoWhenThatHabitDoesNotExist() throws Exception {
        String userIdStr = "10";

        mockMvc.perform(get("/api/v1/habits/1/reports-info")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This user doesn't have access to this habit"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canGetReportAtDayWhenUserIsCreator() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;
        LocalDate date = LocalDate.of(2025, 4, 11);

        Habit habit = Habit.builder()
                .userId(userId)
                .name("Название")
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .build();

        habitRepository.save(habit);

        Mockito.when(reportClient.getReportAtDay(testInternalToken, 1L, date)).thenReturn(ResponseEntity.ok(
                ReportFullInfoResponse.builder()
                        .reportId(null)
                        .isCompleted(false)
                        .completionTime(null)
                        .photoUrl(null)
                        .build()
        ));

        mockMvc.perform(get("/api/v1/habits/1/get-report/at-day/2025-04-11")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").doesNotExist())
                .andExpect(jsonPath("$.completed").value("false"))
                .andExpect(jsonPath("$.completionTime").doesNotExist())
                .andExpect(jsonPath("$.photoUrl").doesNotExist());
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canGetReportAtDayWhenUserIsSubscriber() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;
        LocalDate date = LocalDate.of(2025, 4, 11);

        // Привычку с ID=1 создал другой юзер
        Habit habit = Habit.builder()
                .userId(12L)
                .name("Название")
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .build();

        habitRepository.save(habit);

        SubscriptionCache subscriptionCache = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(1L)
                        .subscriberId(userId)
                        .build())
                .creatorLogin("user12")
                .build();

        subscriptionCacheRepository.save(subscriptionCache);

        Mockito.when(reportClient.getReportAtDay(testInternalToken, 1L, date)).thenReturn(ResponseEntity.ok(
                ReportFullInfoResponse.builder()
                        .reportId(null)
                        .isCompleted(false)
                        .completionTime(null)
                        .photoUrl(null)
                        .build()
        ));

        mockMvc.perform(get("/api/v1/habits/1/get-report/at-day/2025-04-11")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").doesNotExist())
                .andExpect(jsonPath("$.completed").value("false"))
                .andExpect(jsonPath("$.completionTime").doesNotExist())
                .andExpect(jsonPath("$.photoUrl").doesNotExist());
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failGetReportAtDayWhenUserIsNotCreatorAndIsNotSubscriber() throws Exception {
        String userIdStr = "10";

        // Привычку с ID=1 создал другой юзер
        Habit habit = Habit.builder()
                .userId(12L)
                .name("Название")
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .build();

        habitRepository.save(habit);

        // На привычку с ID=1 подписан другой юзер
        SubscriptionCache subscriptionCache = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(1L)
                        .subscriberId(11L)
                        .build())
                .creatorLogin("user12")
                .build();

        subscriptionCacheRepository.save(subscriptionCache);

        mockMvc.perform(get("/api/v1/habits/1/get-report/at-day/2025-04-11")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This user doesn't have access to this habit"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failGetReportAtDayWhenThatHabitDoesNotExist() throws Exception {
        String userIdStr = "10";

        mockMvc.perform(get("/api/v1/habits/1/get-report/at-day/2025-04-11")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This user doesn't have access to this habit"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canGetGeneralInfoWhenUserIsCreatorWithoutDuration() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;

        LocalDateTime createdAt = TODAY_DATE.atStartOfDay();

        HabitWithoutAutoCreationTime existingHabit = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .durationDays(null)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit);

        mockMvc.perform(get("/api/v1/habits/1/general-info")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Название"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.isPhotoAllowed").value("false"))
                .andExpect(jsonPath("$.isHarmful").value("false"))
                .andExpect(jsonPath("$.durationDays").doesNotExist())
                .andExpect(jsonPath("$.howManyDaysLeft").doesNotExist())
                .andExpect(jsonPath("$.frequencyType").value("WEEKLY_ON_DAYS"))
                .andExpect(jsonPath("$.daysOfWeek").isArray())
                .andExpect(jsonPath("$.timesPerWeek").doesNotExist())
                .andExpect(jsonPath("$.timesPerMonth").doesNotExist())
                .andExpect(jsonPath("$.createdAt", containsString(createdAt.toString())))
                .andExpect(jsonPath("$.subscribersCount").value(0));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canGetGeneralInfoWhenUserIsCreatorWithDurationWhenHabitIsCreatedToday() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;

        LocalDateTime createdAt = TODAY_DATE.atStartOfDay();

        HabitWithoutAutoCreationTime existingHabit = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .durationDays(3)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit);

        subscriptionCacheRepository.save(
                SubscriptionCache.builder()
                        .id(SubscriptionCacheId.builder()
                                .habitId(1L)
                                .subscriberId(11L)
                                .build())
                        .creatorLogin("user10")
                        .build()
        );

        mockMvc.perform(get("/api/v1/habits/1/general-info")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Название"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.isPhotoAllowed").value("false"))
                .andExpect(jsonPath("$.isHarmful").value("false"))
                .andExpect(jsonPath("$.durationDays").value(3))
                .andExpect(jsonPath("$.howManyDaysLeft").value(3))
                .andExpect(jsonPath("$.frequencyType").value("WEEKLY_ON_DAYS"))
                .andExpect(jsonPath("$.daysOfWeek").isArray())
                .andExpect(jsonPath("$.timesPerWeek").doesNotExist())
                .andExpect(jsonPath("$.timesPerMonth").doesNotExist())
                .andExpect(jsonPath("$.createdAt", containsString(createdAt.toString())))
                .andExpect(jsonPath("$.subscribersCount").value(1));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canGetGeneralInfoWhenUserIsSubscriberWithDurationWhenHabitIsExpired() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;

        LocalDateTime createdAt = TODAY_DATE.minusDays(4).atStartOfDay();

        // Привычку с ID=1 создал другой юзер
        HabitWithoutAutoCreationTime existingHabit = HabitWithoutAutoCreationTime.builder()
                .userId(13L)
                .name("Название")
                .frequencyType(MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(2)
                .durationDays(3)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit);

        // На привычку с ID=1 подписан текущий юзер
        subscriptionCacheRepository.save(
                SubscriptionCache.builder()
                        .id(SubscriptionCacheId.builder()
                                .habitId(1L)
                                .subscriberId(userId)
                                .build())
                        .creatorLogin("user13")
                        .build()
        );

        // На другую привычку (с ID=2) подписан текущий юзер
        subscriptionCacheRepository.save(
                SubscriptionCache.builder()
                        .id(SubscriptionCacheId.builder()
                                .habitId(2L)
                                .subscriberId(userId)
                                .build())
                        .creatorLogin("user13")
                        .build()
        );

        mockMvc.perform(get("/api/v1/habits/1/general-info")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Название"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.isPhotoAllowed").value("false"))
                .andExpect(jsonPath("$.isHarmful").value("false"))
                .andExpect(jsonPath("$.durationDays").value(3))
                .andExpect(jsonPath("$.howManyDaysLeft").value(-1))
                .andExpect(jsonPath("$.frequencyType").value("MONTHLY_X_TIMES"))
                .andExpect(jsonPath("$.daysOfWeek").doesNotExist())
                .andExpect(jsonPath("$.timesPerWeek").doesNotExist())
                .andExpect(jsonPath("$.timesPerMonth").value(2))
                .andExpect(jsonPath("$.createdAt", containsString(createdAt.toString())))
                .andExpect(jsonPath("$.subscribersCount").value(1));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failGetGeneralInfoWhenUserIsNotCreatorAndIsNotSubscriber() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;

        LocalDateTime createdAt = TODAY_DATE.atStartOfDay();

        // Привычку с ID=1 создал другой юзер
        HabitWithoutAutoCreationTime existingHabit = HabitWithoutAutoCreationTime.builder()
                .userId(13L)
                .name("Название")
                .frequencyType(MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(2)
                .durationDays(3)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit);

        // На привычку с ID=1 подписан другой юзер
        subscriptionCacheRepository.save(
                SubscriptionCache.builder()
                        .id(SubscriptionCacheId.builder()
                                .habitId(1L)
                                .subscriberId(11L)
                                .build())
                        .creatorLogin("user13")
                        .build()
        );

        // На другую привычку (с ID=2) подписан текущий юзер
        subscriptionCacheRepository.save(
                SubscriptionCache.builder()
                        .id(SubscriptionCacheId.builder()
                                .habitId(2L)
                                .subscriberId(userId)
                                .build())
                        .creatorLogin("user13")
                        .build()
        );

        mockMvc.perform(get("/api/v1/habits/1/general-info")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This user doesn't have access to this habit"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failGetGeneralInfoWhenThatHabitDoesNotExist() throws Exception {
        String userIdStr = "10";

        mockMvc.perform(get("/api/v1/habits/1/general-info")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This user doesn't have access to this habit"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testGetAllUserHabitsAtDayWhenThereAreNoHabitsOfThatUser() throws Exception {
        String userIdStr = "10";

        LocalDateTime createdAt = TODAY_DATE.atStartOfDay();

        // Эту привычку создал другой юзер
        HabitWithoutAutoCreationTime existingHabit = HabitWithoutAutoCreationTime.builder()
                .userId(11L)
                .name("Название 1")
                .frequencyType(MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(5)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit);

        mockMvc.perform(get("/api/v1/habits/all-user-habits/at-day/2025-04-12")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testGetAllUserHabitsAtDayWhenThereAreNoCurrentHabitsOfThatUser() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;

        LocalDateTime createdAt = TODAY_DATE.minusDays(3).atStartOfDay();

        // Эта привычка не будет текущей в TODAY_DATE (так как неподходящий день недели)
        HabitWithoutAutoCreationTime existingHabit1 = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название 1")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.FRIDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit1);

        // Эта привычка не будет текущей в TODAY_DATE (так как истекла длительность)
        HabitWithoutAutoCreationTime existingHabit2 = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название 2")
                .frequencyType(WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(3)
                .timesPerMonth(null)
                .durationDays(1)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit2);

        mockMvc.perform(get("/api/v1/habits/all-user-habits/at-day/2025-04-12")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testGetAllUserHabitsAtDayWhenThereAreCurrentWeeklyOnDaysHabits() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;

        LocalDateTime createdAt = TODAY_DATE.minusDays(3).atStartOfDay();

        // Эта привычка не будет текущей в TODAY_DATE (так как неподходящий день недели)
        HabitWithoutAutoCreationTime existingHabit1 = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название 1")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.FRIDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit1);

        // Эта привычка будет текущей в TODAY_DATE (на эту привычку не будет подписчиков)
        HabitWithoutAutoCreationTime existingHabit2 = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название 2")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.SATURDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit2);

        // Эта привычка будет текущей в TODAY_DATE (на эту привычку будут подписчики)
        HabitWithoutAutoCreationTime existingHabit3 = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название 3")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit3);

        // Подписка на привычку existingHabit3
        SubscriptionCache subscription = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(3L)
                        .subscriberId(11L)
                        .build())
                .creatorLogin("user10")
                .build();

        subscriptionCacheRepository.save(subscription);

        Mockito.when(reportClient.isCompletedAtDay(testInternalToken, 2L, TODAY_DATE)).thenReturn(ResponseEntity.ok(false));

        Mockito.when(reportClient.isCompletedAtDay(testInternalToken, 3L, TODAY_DATE)).thenReturn(ResponseEntity.ok(true));

        List<HabitShortInfoResponse> expectedList = List.of(
                HabitShortInfoResponse.builder()
                        .habitId(2L)
                        .name("Название 2")
                        .isCompleted(false)
                        .subscribersCount(0)
                        .frequencyType(WEEKLY_ON_DAYS)
                        .completionsInPeriod(null)
                        .completionsPlannedInPeriod(null)
                        .build(),
                HabitShortInfoResponse.builder()
                        .habitId(3L)
                        .name("Название 3")
                        .isCompleted(true)
                        .subscribersCount(1)
                        .frequencyType(WEEKLY_ON_DAYS)
                        .completionsInPeriod(null)
                        .completionsPlannedInPeriod(null)
                        .build()
        );

        String expectedJson = objectMapper.writeValueAsString(expectedList);

        mockMvc.perform(get("/api/v1/habits/all-user-habits/at-day/2025-04-12")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testGetAllUserHabitsAtDayWhenThereAreCurrentWeeklyXTimesAndMonthlyXTimesHabits() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;

        LocalDateTime createdAt = TODAY_DATE.minusDays(3).atStartOfDay();

        // Эта привычка не будет текущей в TODAY_DATE (так как истекла длительность)
        HabitWithoutAutoCreationTime existingHabit1 = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название 1")
                .frequencyType(WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(7)
                .timesPerMonth(null)
                .durationDays(3)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit1);

        // Эта привычка будет текущей в TODAY_DATE
        HabitWithoutAutoCreationTime existingHabit2 = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название 2")
                .frequencyType(WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(1)
                .timesPerMonth(null)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit2);

        // Эта привычка будет текущей в TODAY_DATE
        HabitWithoutAutoCreationTime existingHabit3 = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название 3")
                .frequencyType(MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(5)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit3);

        Mockito.when(reportClient.isCompletedAtDay(testInternalToken, 2L, TODAY_DATE)).thenReturn(ResponseEntity.ok(false));

        Mockito.when(reportClient.countCompletionsInPeriod(testInternalToken, 2L, Period.WEEK, TODAY_DATE))
                .thenReturn(ResponseEntity.ok(0));

        Mockito.when(reportClient.isCompletedAtDay(testInternalToken, 3L, TODAY_DATE)).thenReturn(ResponseEntity.ok(true));

        Mockito.when(reportClient.countCompletionsInPeriod(testInternalToken, 3L, Period.MONTH, TODAY_DATE))
                .thenReturn(ResponseEntity.ok(2));

        List<HabitShortInfoResponse> expectedList = List.of(
                HabitShortInfoResponse.builder()
                        .habitId(2L)
                        .name("Название 2")
                        .isCompleted(false)
                        .subscribersCount(0)
                        .frequencyType(WEEKLY_X_TIMES)
                        .completionsInPeriod(0)
                        .completionsPlannedInPeriod(1)
                        .build(),
                HabitShortInfoResponse.builder()
                        .habitId(3L)
                        .name("Название 3")
                        .isCompleted(true)
                        .subscribersCount(0)
                        .frequencyType(MONTHLY_X_TIMES)
                        .completionsInPeriod(2)
                        .completionsPlannedInPeriod(5)
                        .build()
        );

        String expectedJson = objectMapper.writeValueAsString(expectedList);

        mockMvc.perform(get("/api/v1/habits/all-user-habits/at-day/2025-04-12")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testGetAllUserSubscribedHabitsAtDayWhenThereAreNoSubscribedHabitsOfThatUser() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;

        LocalDateTime createdAt = TODAY_DATE.atStartOfDay();

        // Это привычка текущего юзера (соответственно он сам на нее не подписан)
        HabitWithoutAutoCreationTime existingHabit1 = HabitWithoutAutoCreationTime.builder()
                .userId(userId)
                .name("Название 1")
                .frequencyType(MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(5)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit1);

        // Кто-то подписан на привычку 1
        SubscriptionCache subscription1 = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(1L)
                        .subscriberId(12L)
                        .build())
                .creatorLogin("user10")
                .build();

        subscriptionCacheRepository.save(subscription1);

        // Это привычка другого юзера, на которую текущий юзер не подписан
        HabitWithoutAutoCreationTime existingHabit2 = HabitWithoutAutoCreationTime.builder()
                .userId(11L)
                .name("Название 2")
                .frequencyType(MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(5)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit2);

        // Другой юзер подписан на привычку 2
        SubscriptionCache subscription2 = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(2L)
                        .subscriberId(12L)
                        .build())
                .creatorLogin("user11")
                .build();

        subscriptionCacheRepository.save(subscription2);

        mockMvc.perform(get("/api/v1/habits/all-user-subscribed-habits/at-day/2025-04-12")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testGetAllUserSubscribedHabitsAtDayWhenThereAreNoCurrentSubscribedHabitsOfThatUser() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;

        LocalDateTime createdAt = TODAY_DATE.atStartOfDay();

        // Это привычка другого юзера, на которую подписан текущий юзер, но она не является текущей в TODAY_DATE (неподходящий день недели)
        HabitWithoutAutoCreationTime existingHabit1 = HabitWithoutAutoCreationTime.builder()
                .userId(11L)
                .name("Название 1")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.FRIDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit1);

        // Текущий юзер подписан на привычку 1
        SubscriptionCache subscription1 = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(1L)
                        .subscriberId(userId)
                        .build())
                .creatorLogin("user11")
                .build();

        subscriptionCacheRepository.save(subscription1);

        // Еще один юзер подписан на привычку 1
        SubscriptionCache subscription2 = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(1L)
                        .subscriberId(20L)
                        .build())
                .creatorLogin("user11")
                .build();

        subscriptionCacheRepository.save(subscription2);

        // Это привычка другого юзера, на которую не подписан текущий юзер, и она является текущей в TODAY_DATE
        HabitWithoutAutoCreationTime existingHabit2 = HabitWithoutAutoCreationTime.builder()
                .userId(11L)
                .name("Название 2")
                .frequencyType(MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(5)
                .createdAt(createdAt)
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit2);

        mockMvc.perform(get("/api/v1/habits/all-user-subscribed-habits/at-day/2025-04-12")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testGetAllUserSubscribedHabitsAtDayWhenThereAreCurrentWeeklyOnDaysSubscribedHabits() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;

        // Это привычка другого юзера, на которую подписан текущий юзер, но она не является текущей в TODAY_DATE (истекла длительность)
        HabitWithoutAutoCreationTime existingHabit1 = HabitWithoutAutoCreationTime.builder()
                .userId(11L)
                .name("Название 1")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.SATURDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .durationDays(3)
                .createdAt(TODAY_DATE.minusDays(3).atStartOfDay())
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit1);

        // Текущий юзер подписан на привычку 1
        SubscriptionCache subscription1 = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(1L)
                        .subscriberId(userId)
                        .build())
                .creatorLogin("user11")
                .build();

        subscriptionCacheRepository.save(subscription1);

        // Это привычка другого юзера, на которую подписан текущий юзер, и она является текущей в TODAY_DATE
        // (на нее не будет больше подписчиков)
        HabitWithoutAutoCreationTime existingHabit2 = HabitWithoutAutoCreationTime.builder()
                .userId(11L)
                .name("Название 2")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(TODAY_DATE.minusDays(30).atStartOfDay())
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit2);

        // Текущий юзер подписан на привычку 2
        SubscriptionCache subscription2 = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(2L)
                        .subscriberId(userId)
                        .build())
                .creatorLogin("user11")
                .build();

        subscriptionCacheRepository.save(subscription2);

        // Это привычка другого юзера, на которую подписан текущий юзер, и она является текущей в TODAY_DATE
        // (на нее будут еще подписчики)
        HabitWithoutAutoCreationTime existingHabit3 = HabitWithoutAutoCreationTime.builder()
                .userId(12L)
                .name("Название 3")
                .frequencyType(WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.SATURDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .createdAt(TODAY_DATE.minusDays(30).atStartOfDay())
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit3);

        // Текущий юзер подписан на привычку 3
        SubscriptionCache subscription3 = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(3L)
                        .subscriberId(userId)
                        .build())
                .creatorLogin("user12")
                .build();

        subscriptionCacheRepository.save(subscription3);

        // Еще один юзер подписан на привычку 3
        SubscriptionCache subscription4 = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(3L)
                        .subscriberId(15L)
                        .build())
                .creatorLogin("user12")
                .build();

        subscriptionCacheRepository.save(subscription4);

        Mockito.when(reportClient.isCompletedAtDay(testInternalToken, 2L, TODAY_DATE)).thenReturn(ResponseEntity.ok(false));

        Mockito.when(reportClient.isCompletedAtDay(testInternalToken, 3L, TODAY_DATE)).thenReturn(ResponseEntity.ok(true));

        List<SubscribedHabitShortInfoResponse> expectedList = List.of(
                SubscribedHabitShortInfoResponse.builder()
                        .habitId(2L)
                        .creatorLogin("user11")
                        .name("Название 2")
                        .isCompleted(false)
                        .subscribersCount(1)
                        .frequencyType(WEEKLY_ON_DAYS)
                        .completionsInPeriod(null)
                        .completionsPlannedInPeriod(null)
                        .build(),
                SubscribedHabitShortInfoResponse.builder()
                        .habitId(3L)
                        .creatorLogin("user12")
                        .name("Название 3")
                        .isCompleted(true)
                        .subscribersCount(2)
                        .frequencyType(WEEKLY_ON_DAYS)
                        .completionsInPeriod(null)
                        .completionsPlannedInPeriod(null)
                        .build()
        );

        String expectedJson = objectMapper.writeValueAsString(expectedList);

        mockMvc.perform(get("/api/v1/habits/all-user-subscribed-habits/at-day/2025-04-12")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testGetAllUserSubscribedHabitsAtDayWhenThereAreCurrentWeeklyXTimesAndMonthlyXTimesSubscribedHabits() throws Exception {
        String userIdStr = "10";
        Long userId = 10L;

        // Это привычка другого юзера, на которую подписан текущий юзер, но она не является текущей в TODAY_DATE (истекла длительность)
        HabitWithoutAutoCreationTime existingHabit1 = HabitWithoutAutoCreationTime.builder()
                .userId(11L)
                .name("Название 1")
                .frequencyType(WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(2)
                .timesPerMonth(null)
                .durationDays(1)
                .createdAt(TODAY_DATE.minusDays(3).atStartOfDay())
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit1);

        // Текущий юзер подписан на привычку 1
        SubscriptionCache subscription1 = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(1L)
                        .subscriberId(userId)
                        .build())
                .creatorLogin("user11")
                .build();

        subscriptionCacheRepository.save(subscription1);

        // Это привычка другого юзера, на которую подписан текущий юзер, и она является текущей в TODAY_DATE
        HabitWithoutAutoCreationTime existingHabit2 = HabitWithoutAutoCreationTime.builder()
                .userId(11L)
                .name("Название 2")
                .frequencyType(WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(5)
                .timesPerMonth(null)
                .createdAt(TODAY_DATE.minusDays(30).atStartOfDay())
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit2);

        // Текущий юзер подписан на привычку 2
        SubscriptionCache subscription2 = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(2L)
                        .subscriberId(userId)
                        .build())
                .creatorLogin("user11")
                .build();

        subscriptionCacheRepository.save(subscription2);

        // Это привычка другого юзера, на которую подписан текущий юзер, и она является текущей в TODAY_DATE
        HabitWithoutAutoCreationTime existingHabit3 = HabitWithoutAutoCreationTime.builder()
                .userId(12L)
                .name("Название 3")
                .frequencyType(MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(10)
                .createdAt(TODAY_DATE.minusDays(30).atStartOfDay())
                .build();

        habitWithoutAutoCreationTimeRepository.save(existingHabit3);

        // Текущий юзер подписан на привычку 3
        SubscriptionCache subscription3 = SubscriptionCache.builder()
                .id(SubscriptionCacheId.builder()
                        .habitId(3L)
                        .subscriberId(userId)
                        .build())
                .creatorLogin("user12")
                .build();

        subscriptionCacheRepository.save(subscription3);

        Mockito.when(reportClient.isCompletedAtDay(testInternalToken, 2L, TODAY_DATE)).thenReturn(ResponseEntity.ok(false));

        Mockito.when(reportClient.countCompletionsInPeriod(testInternalToken, 2L, Period.WEEK, TODAY_DATE)).thenReturn(ResponseEntity.ok(0));

        Mockito.when(reportClient.isCompletedAtDay(testInternalToken, 3L, TODAY_DATE)).thenReturn(ResponseEntity.ok(true));

        Mockito.when(reportClient.countCompletionsInPeriod(testInternalToken, 3L, Period.MONTH, TODAY_DATE)).thenReturn(ResponseEntity.ok(5));

        List<SubscribedHabitShortInfoResponse> expectedList = List.of(
                SubscribedHabitShortInfoResponse.builder()
                        .habitId(2L)
                        .creatorLogin("user11")
                        .name("Название 2")
                        .isCompleted(false)
                        .subscribersCount(1)
                        .frequencyType(WEEKLY_X_TIMES)
                        .completionsInPeriod(0)
                        .completionsPlannedInPeriod(5)
                        .build(),
                SubscribedHabitShortInfoResponse.builder()
                        .habitId(3L)
                        .creatorLogin("user12")
                        .name("Название 3")
                        .isCompleted(true)
                        .subscribersCount(1)
                        .frequencyType(MONTHLY_X_TIMES)
                        .completionsInPeriod(5)
                        .completionsPlannedInPeriod(10)
                        .build()
        );

        String expectedJson = objectMapper.writeValueAsString(expectedList);

        mockMvc.perform(get("/api/v1/habits/all-user-subscribed-habits/at-day/2025-04-12")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

}
