package com.vladmikhayl.report.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vladmikhayl.report.dto.ReportCreationRequest;
import com.vladmikhayl.report.service.ReportService;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController underTest;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(underTest).build();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    @Test
    void canCreateReportWithoutPhoto() throws Exception {
        Long habitId = 2L;

        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl(null)
                .build();

        String userIdStr = "2";

        doNothing().when(reportService).createReport(any(ReportCreationRequest.class), eq(userIdStr));

        mockMvc.perform(post("/api/v1/reports/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isCreated());

        verify(reportService).createReport(argThat(req -> req.equals(request)), eq(userIdStr));
    }

    @Test
    void canCreateReportWithPhoto() throws Exception {
        Long habitId = 2L;

        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl("https://github.com/")
                .build();

        String userIdStr = "2";

        doNothing().when(reportService).createReport(any(ReportCreationRequest.class), eq(userIdStr));

        mockMvc.perform(post("/api/v1/reports/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isCreated());

        verify(reportService).createReport(argThat(req -> req.equals(request)), eq(userIdStr));
    }

    @Test
    void failCreateReportWithoutHabitId() throws Exception {
        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(null)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl(null)
                .build();

        String userIdStr = "2";

        mockMvc.perform(post("/api/v1/reports/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Habit ID must be specified"));
                })
                .andExpect(status().isBadRequest());

        verify(reportService, never()).createReport(any(), any());
    }

    @Test
    void failCreateReportWithoutDate() throws Exception {
        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(2L)
                .date(null)
                .photoUrl(null)
                .build();

        String userIdStr = "2";

        mockMvc.perform(post("/api/v1/reports/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Date must be specified"));
                })
                .andExpect(status().isBadRequest());

        verify(reportService, never()).createReport(any(), any());
    }

}