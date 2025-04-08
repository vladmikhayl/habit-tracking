package com.vladmikhayl.habit.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmikhayl.habit.dto.HabitCreationRequest;
import com.vladmikhayl.habit.dto.HabitEditingRequest;
import com.vladmikhayl.habit.entity.FrequencyType;
import com.vladmikhayl.habit.entity.Habit;
import com.vladmikhayl.habit.repository.HabitRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
@EmbeddedKafka(partitions = 1, topics = {"habit-with-photo-allowed-created"})
@AutoConfigureMockMvc
public class HabitControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HabitRepository habitRepository;

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
                .name("Новое название")
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
                .name("Новое название")
                .description("Описание")
                .isPhotoAllowed(true)
                .isHarmful(true)
                .durationDays(30)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        Optional<Habit> habit = habitRepository.findByName("Новое название");
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
                .name(null)
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
                .name("Новое название")
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

    @Test
    @Sql(statements = "ALTER SEQUENCE habit_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failEditHabitWhenUserAlreadyHasHabitWithSameName() throws Exception {
        String userIdStr = "1";
        Long userId = 1L;

        Habit existingHabit1 = Habit.builder()
                .userId(userId)
                .name("Название 1")
                .description(null)
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(5)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        habitRepository.save(existingHabit1);

        Habit existingHabit2 = Habit.builder()
                .userId(userId)
                .name("Название 2")
                .description(null)
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(5)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        habitRepository.save(existingHabit2);

        HabitEditingRequest request = HabitEditingRequest.builder()
                .name("Название 1")
                .description(null)
                .isHarmful(null)
                .durationDays(null)
                .build();

        mockMvc.perform(put("/api/v1/habits/2/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("This user already has a habit with that name"));

        Optional<Habit> oldHabit = habitRepository.findByName("Название 2");
        assertThat(oldHabit.isPresent()).isTrue();

        assertThat(oldHabit.get())
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt")
                .isEqualTo(existingHabit2);

        long habitsCount = habitRepository.count();
        assertThat(habitsCount).isEqualTo(2);
    }

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
                .name(null)
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

}
