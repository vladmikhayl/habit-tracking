package com.vladmikhayl.report.service;

import com.vladmikhayl.report.dto.ReportCreationRequest;
import com.vladmikhayl.report.entity.Report;
import com.vladmikhayl.report.repository.HabitPhotoAllowedCacheRepository;
import com.vladmikhayl.report.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private HabitPhotoAllowedCacheRepository habitPhotoAllowedCacheRepository;

    @InjectMocks
    private ReportService underTest;

    @Test
    void canCreateReportWithoutPhoto() {
        Long habitId = 1L;

        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl(null)
                .build();

        String userIdStr = "2";
        Long userId = 2L;

        when(reportRepository.existsByHabitIdAndDate(habitId, request.getDate())).thenReturn(false);

        underTest.createReport(request, userIdStr);

        ArgumentCaptor<Report> reportArgumentCaptor = ArgumentCaptor.forClass(Report.class);

        verify(reportRepository).save(reportArgumentCaptor.capture());

        Report capturedReport = reportArgumentCaptor.getValue();

        Report expected = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl(null)
                .build();

        assertThat(capturedReport)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt")
                .isEqualTo(expected);
    }

    @Test
    void canCreateReportWithPhotoWhenPhotoIsAllowed() {
        Long habitId = 1L;

        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 20))
                .photoUrl("https://github.com/")
                .build();

        String userIdStr = "2";
        Long userId = 2L;

        when(reportRepository.existsByHabitIdAndDate(habitId, request.getDate())).thenReturn(false);

        when(habitPhotoAllowedCacheRepository.existsById(habitId)).thenReturn(true);

        underTest.createReport(request, userIdStr);

        ArgumentCaptor<Report> reportArgumentCaptor = ArgumentCaptor.forClass(Report.class);

        verify(reportRepository).save(reportArgumentCaptor.capture());

        Report capturedReport = reportArgumentCaptor.getValue();

        Report expected = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 20))
                .photoUrl("https://github.com/")
                .build();

        assertThat(capturedReport)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt")
                .isEqualTo(expected);
    }

    @Test
    void failCreateReportWithPhotoWhenPhotoIsNotAllowed() {
        Long habitId = 1L;

        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 20))
                .photoUrl("https://github.com/")
                .build();

        String userIdStr = "2";

        when(reportRepository.existsByHabitIdAndDate(habitId, request.getDate())).thenReturn(false);

        when(habitPhotoAllowedCacheRepository.existsById(habitId)).thenReturn(false);

        assertThatThrownBy(() -> underTest.createReport(request, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                })
                .hasMessageContaining("This habit doesn't imply a photo, but it was attached");

        verify(reportRepository, never()).save(any());
    }

    @Test
    void failCreateReportWithFutureDate() {
        Long habitId = 1L;

        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(habitId)
                .date(LocalDate.now().plusDays(1))
                .photoUrl(null)
                .build();

        String userIdStr = "2";

        when(reportRepository.existsByHabitIdAndDate(habitId, request.getDate())).thenReturn(false);

        assertThatThrownBy(() -> underTest.createReport(request, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                })
                .hasMessageContaining("It is forbidden to mark a habit as completed for a day that has not yet arrived");

        verify(reportRepository, never()).save(any());
    }

    @Test
    void failCreateReportThatHasAlreadyBeenCreated() {
        Long habitId = 1L;

        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 20))
                .photoUrl(null)
                .build();

        String userIdStr = "2";

        when(reportRepository.existsByHabitIdAndDate(habitId, request.getDate())).thenReturn(true);

        assertThatThrownBy(() -> underTest.createReport(request, userIdStr))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("This habit has already been marked as completed on this day");

        verify(reportRepository, never()).save(any());
    }

    // TODO: тест на createReport когда у юзера нет такой привычки в этот день

    // TODO: тест на createReport когда некорректный URL фото

}