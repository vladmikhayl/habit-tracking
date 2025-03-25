package com.vladmikhayl.habit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmikhayl.habit.dto.HabitCreationRequest;
import com.vladmikhayl.habit.dto.HabitEditingRequest;
import com.vladmikhayl.habit.entity.FrequencyType;
import com.vladmikhayl.habit.service.HabitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.DayOfWeek;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HabitControllerTest {

    @Mock
    private HabitService habitService;

    @InjectMocks
    private HabitController underTest;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(underTest).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void canCreateCorrectHabitWithMaxInfo() throws Exception {
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

        doNothing().when(habitService).createHabit(any(HabitCreationRequest.class), eq(userIdStr));

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isCreated());

        verify(habitService).createHabit(argThat(req -> req.equals(request)), eq(userIdStr));
    }

    @Test
    void canCreateCorrectHabitWithMinInfo() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Название")
                .description(null)
                .isPhotoAllowed(false)
                .isHarmful(false)
                .durationDays(null)
                .frequencyType(FrequencyType.MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(1)
                .build();

        String userIdStr = "1";

        doNothing().when(habitService).createHabit(any(HabitCreationRequest.class), eq(userIdStr));

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isCreated());

        verify(habitService).createHabit(argThat(req -> req.equals(request)), eq(userIdStr));
    }

    @Test
    void failCreateHabitWithBlankName() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("")
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

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Name cannot be blank"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).createHabit(any(), any());
    }

    @Test
    void failCreateHabitWithTooLongName() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("А".repeat(256))
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

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Name must not exceed 255 characters"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).createHabit(any(), any());
    }

    @Test
    void failCreateHabitWithTooLongDescription() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Название")
                .description("А".repeat(1001))
                .isPhotoAllowed(true)
                .isHarmful(true)
                .durationDays(30)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        String userIdStr = "1";

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Description must not exceed 1000 characters"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).createHabit(any(), any());
    }

    @Test
    void failCreateHabitWithTooLittleDurationDays() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Название")
                .description("Описание")
                .isPhotoAllowed(true)
                .isHarmful(true)
                .durationDays(0)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        String userIdStr = "1";

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Duration must be at least 1 day, if it is provided"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).createHabit(any(), any());
    }

    @Test
    void failCreateHabitWithTooBigDurationDays() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Название")
                .description("Описание")
                .isPhotoAllowed(true)
                .isHarmful(true)
                .durationDays(731)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        String userIdStr = "1";

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Duration must not exceed 730 days"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).createHabit(any(), any());
    }

    @Test
    void failCreateHabitWithNullFrequencyType() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Название")
                .description("Описание")
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(30)
                .frequencyType(null)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        String userIdStr = "1";

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Frequency type must be specified"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).createHabit(any(), any());
    }

    @Test
    void failCreateHabitWithWeeklyOnDaysFrequencyButNullDaysOfWeek() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Название")
                .description("Описание")
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(30)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        String userIdStr = "1";

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Invalid frequency settings"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).createHabit(any(), any());
    }

    @Test
    void failCreateHabitWithWeeklyOnDaysFrequencyButEmptyDaysOfWeek() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Название")
                .description("Описание")
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(30)
                .frequencyType(FrequencyType.WEEKLY_ON_DAYS)
                .daysOfWeek(Set.of())
                .timesPerWeek(null)
                .timesPerMonth(null)
                .build();

        String userIdStr = "1";

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Invalid frequency settings"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).createHabit(any(), any());
    }

    @Test
    void failCreateHabitWithWrongWeeklyXTimesConfig() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Название")
                .description("Описание")
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(30)
                .frequencyType(FrequencyType.WEEKLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(5)
                .timesPerMonth(10)
                .build();

        String userIdStr = "1";

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Invalid frequency settings"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).createHabit(any(), any());
    }

    @Test
    void failCreateHabitWithWrongMonthlyXTimesConfig() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Название")
                .description("Описание")
                .isPhotoAllowed(true)
                .isHarmful(false)
                .durationDays(30)
                .frequencyType(FrequencyType.MONTHLY_X_TIMES)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                .timesPerWeek(null)
                .timesPerMonth(10)
                .build();

        String userIdStr = "1";

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Invalid frequency settings"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).createHabit(any(), any());
    }

    @Test
    void failCreateHabitWithWrongHarmfulSetting() throws Exception {
        HabitCreationRequest request = HabitCreationRequest.builder()
                .name("Название")
                .description("Описание")
                .isPhotoAllowed(true)
                .isHarmful(true)
                .durationDays(30)
                .frequencyType(FrequencyType.MONTHLY_X_TIMES)
                .daysOfWeek(null)
                .timesPerWeek(null)
                .timesPerMonth(10)
                .build();

        String userIdStr = "1";

        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("A habit with this FrequencyType cannot be harmful"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).createHabit(any(), any());
    }

    @Test
    void failCreateHabitWhenRequestBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/habits/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void canMaxEditHabit() throws Exception {
        HabitEditingRequest request = HabitEditingRequest.builder()
                .name("Название")
                .description("Описание")
                .isPhotoAllowed(true)
                .isHarmful(true)
                .durationDays(30)
                .build();

        String userIdStr = "1";
        Long habitId = 10L;

        doNothing().when(habitService).editHabit(eq(habitId), any(HabitEditingRequest.class), eq(userIdStr));

        mockMvc.perform(put("/api/v1/habits/10/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk());

        verify(habitService).editHabit(eq(habitId), argThat(req -> req.equals(request)), eq(userIdStr));
    }

    @Test
    void canEditHabitWithDurationDaysEqualsToZero() throws Exception {
        HabitEditingRequest request = HabitEditingRequest.builder()
                .name(null)
                .description(null)
                .isPhotoAllowed(null)
                .isHarmful(null)
                .durationDays(0)
                .build();

        String userIdStr = "1";
        Long habitId = 10L;

        doNothing().when(habitService).editHabit(eq(habitId), any(HabitEditingRequest.class), eq(userIdStr));

        mockMvc.perform(put("/api/v1/habits/10/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk());

        verify(habitService).editHabit(eq(habitId), argThat(req -> req.equals(request)), eq(userIdStr));
    }

    @Test
    void canNullEditHabit() throws Exception {
        HabitEditingRequest request = HabitEditingRequest.builder()
                .name(null)
                .description(null)
                .isPhotoAllowed(null)
                .isHarmful(null)
                .durationDays(null)
                .build();

        String userIdStr = "1";
        Long habitId = 10L;

        doNothing().when(habitService).editHabit(eq(habitId), any(HabitEditingRequest.class), eq(userIdStr));

        mockMvc.perform(put("/api/v1/habits/10/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk());

        verify(habitService).editHabit(eq(habitId), argThat(req -> req.equals(request)), eq(userIdStr));
    }

    @Test
    void failEditHabitWithBlankName() throws Exception {
        HabitEditingRequest request = HabitEditingRequest.builder()
                .name("")
                .description(null)
                .isPhotoAllowed(null)
                .isHarmful(null)
                .durationDays(null)
                .build();

        String userIdStr = "1";

        mockMvc.perform(put("/api/v1/habits/10/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Name cannot be blank"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).editHabit(any(), any(), any());
    }

    @Test
    void failEditHabitWithTooLongName() throws Exception {
        HabitEditingRequest request = HabitEditingRequest.builder()
                .name("А".repeat(256))
                .description(null)
                .isPhotoAllowed(null)
                .isHarmful(null)
                .durationDays(null)
                .build();

        String userIdStr = "1";

        mockMvc.perform(put("/api/v1/habits/10/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Name must not exceed 255 characters"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).editHabit(any(), any(), any());
    }

    @Test
    void failEditHabitWithTooLongDescription() throws Exception {
        HabitEditingRequest request = HabitEditingRequest.builder()
                .name(null)
                .description("А".repeat(1001))
                .isPhotoAllowed(null)
                .isHarmful(null)
                .durationDays(null)
                .build();

        String userIdStr = "1";

        mockMvc.perform(put("/api/v1/habits/10/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Description must not exceed 1000 characters"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).editHabit(any(), any(), any());
    }

    @Test
    void failEditHabitWithTooLittleDurationDays() throws Exception {
        HabitEditingRequest request = HabitEditingRequest.builder()
                .name(null)
                .description(null)
                .isPhotoAllowed(null)
                .isHarmful(null)
                .durationDays(-1)
                .build();

        String userIdStr = "1";

        mockMvc.perform(put("/api/v1/habits/10/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Duration must be at least 1 day, if it is provided"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).editHabit(any(), any(), any());
    }

    @Test
    void failEditHabitWithTooBigDurationDays() throws Exception {
        HabitEditingRequest request = HabitEditingRequest.builder()
                .name(null)
                .description(null)
                .isPhotoAllowed(null)
                .isHarmful(null)
                .durationDays(731)
                .build();

        String userIdStr = "1";

        mockMvc.perform(put("/api/v1/habits/10/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Duration must not exceed 730 days"));
                })
                .andExpect(status().isBadRequest());

        verify(habitService, never()).editHabit(any(), any(), any());
    }

    @Test
    void failEditHabitWhenRequestBodyIsMissing() throws Exception {
        mockMvc.perform(put("/api/v1/habits/10/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1"))
                .andExpect(status().isBadRequest());
    }

}