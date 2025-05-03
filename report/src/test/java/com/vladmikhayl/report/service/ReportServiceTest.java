package com.vladmikhayl.report.service;

import com.vladmikhayl.report.dto.request.ReportCreationRequest;
import com.vladmikhayl.report.dto.request.ReportPhotoEditingRequest;
import com.vladmikhayl.report.entity.Report;
import com.vladmikhayl.report.repository.HabitPhotoAllowedCacheRepository;
import com.vladmikhayl.report.repository.ReportRepository;
import com.vladmikhayl.report.service.feign.HabitClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Value("${internal.token}")
    private String testInternalToken;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private HabitPhotoAllowedCacheRepository habitPhotoAllowedCacheRepository;

    @Mock
    private HabitClient habitClient;

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

        when(habitClient.isCurrent(testInternalToken, habitId, userId, request.getDate())).thenReturn(ResponseEntity.ok(true));

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

        when(habitClient.isCurrent(testInternalToken, habitId, userId, request.getDate())).thenReturn(ResponseEntity.ok(true));

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
        Long userId = 2L;

        when(habitClient.isCurrent(testInternalToken, habitId, userId, request.getDate())).thenReturn(ResponseEntity.ok(true));

        when(reportRepository.existsByHabitIdAndDate(habitId, request.getDate())).thenReturn(false);

        when(habitPhotoAllowedCacheRepository.existsById(habitId)).thenReturn(false);

        assertThatThrownBy(() -> underTest.createReport(request, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                })
                .hasMessageContaining("К отчёту было прикреплено фото, хотя эта привычка не подразумевает фотоотчёты");

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
        Long userId = 2L;

        when(habitClient.isCurrent(testInternalToken, habitId, userId, request.getDate())).thenReturn(ResponseEntity.ok(true));

        when(reportRepository.existsByHabitIdAndDate(habitId, request.getDate())).thenReturn(false);

        assertThatThrownBy(() -> underTest.createReport(request, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                })
                .hasMessageContaining("Этот день ещё не наступил");

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
        Long userId = 2L;

        when(habitClient.isCurrent(testInternalToken, habitId, userId, request.getDate())).thenReturn(ResponseEntity.ok(true));

        when(reportRepository.existsByHabitIdAndDate(habitId, request.getDate())).thenReturn(true);

        assertThatThrownBy(() -> underTest.createReport(request, userIdStr))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("Эта привычка уже отмечена как выполненная в указанный день");

        verify(reportRepository, never()).save(any());
    }

    @Test
    void failCreateReportWhenUserDoesNotHaveThatHabitAtThatDay() {
        Long habitId = 1L;

        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 20))
                .photoUrl(null)
                .build();

        String userIdStr = "2";
        Long userId = 2L;

        when(habitClient.isCurrent(testInternalToken, habitId, userId, request.getDate())).thenReturn(ResponseEntity.ok(false));

        assertThatThrownBy(() -> underTest.createReport(request, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .hasMessageContaining("Эта привычка не является текущей в указанный день для текущего пользователя");

        verify(reportRepository, never()).save(any());
    }

    // TODO: тест на createReport когда некорректный URL фото

    @Test
    void canChangeReportPhotoWhenItHasAlreadyBeenAttached() {
        String userIdStr = "3";
        Long userId = 3L;
        Long reportId = 2L;
        Long habitId = 5L;

        Report report = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 27))
                .photoUrl("https://chatgpt.com/")
                .build();

        ReportPhotoEditingRequest request = ReportPhotoEditingRequest.builder()
                .photoUrl("https://dzen.ru/")
                .build();

        when(reportRepository.findByIdAndUserId(reportId, userId)).thenReturn(Optional.of(report));
        when(habitPhotoAllowedCacheRepository.existsById(habitId)).thenReturn(true);

        underTest.changeReportPhoto(reportId, request, userIdStr);

        assertThat(report.getPhotoUrl()).isEqualTo("https://dzen.ru/");
    }

    @Test
    void canChangeReportPhotoWhenItHasNotBeenAttachedYet() {
        String userIdStr = "3";
        Long userId = 3L;
        Long reportId = 2L;
        Long habitId = 5L;

        Report report = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 27))
                .photoUrl(null)
                .build();

        ReportPhotoEditingRequest request = ReportPhotoEditingRequest.builder()
                .photoUrl("https://dzen.ru/")
                .build();

        when(reportRepository.findByIdAndUserId(reportId, userId)).thenReturn(Optional.of(report));
        when(habitPhotoAllowedCacheRepository.existsById(habitId)).thenReturn(true);

        underTest.changeReportPhoto(reportId, request, userIdStr);

        assertThat(report.getPhotoUrl()).isEqualTo("https://dzen.ru/");
    }

    @Test
    void canChangeReportPhotoToNull() {
        String userIdStr = "3";
        Long userId = 3L;
        Long reportId = 2L;
        Long habitId = 5L;

        Report report = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 27))
                .photoUrl("https://chatgpt.com/")
                .build();

        ReportPhotoEditingRequest request = ReportPhotoEditingRequest.builder()
                .photoUrl("")
                .build();

        when(reportRepository.findByIdAndUserId(reportId, userId)).thenReturn(Optional.of(report));
        when(habitPhotoAllowedCacheRepository.existsById(habitId)).thenReturn(true);

        underTest.changeReportPhoto(reportId, request, userIdStr);

        assertThat(report.getPhotoUrl()).isEqualTo(null);
    }

    @Test
    void canNullChangeReportPhoto() {
        String userIdStr = "3";
        Long userId = 3L;
        Long reportId = 2L;
        Long habitId = 5L;

        Report report = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 27))
                .photoUrl("https://chatgpt.com/")
                .build();

        ReportPhotoEditingRequest request = ReportPhotoEditingRequest.builder()
                .photoUrl(null)
                .build();

        when(reportRepository.findByIdAndUserId(reportId, userId)).thenReturn(Optional.of(report));
        when(habitPhotoAllowedCacheRepository.existsById(habitId)).thenReturn(true);

        underTest.changeReportPhoto(reportId, request, userIdStr);

        assertThat(report.getPhotoUrl()).isEqualTo("https://chatgpt.com/");
    }

    @Test
    void failChangeReportPhotoWhenUserDoesNotHaveThisReport() {
        String userIdStr = "3";
        Long userId = 3L;
        Long reportId = 2L;

        ReportPhotoEditingRequest request = ReportPhotoEditingRequest.builder()
                .photoUrl("https://dzen.ru/")
                .build();

        when(reportRepository.findByIdAndUserId(reportId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.changeReportPhoto(reportId, request, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .hasMessageContaining("У текущего пользователя отсутствует указанный отчёт");
    }

    @Test
    void failChangeReportPhotoWhenThisHabitDoesNotImplyPhotos() {
        String userIdStr = "3";
        Long userId = 3L;
        Long reportId = 2L;
        Long habitId = 5L;

        Report report = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 27))
                .photoUrl("https://chatgpt.com/")
                .build();

        ReportPhotoEditingRequest request = ReportPhotoEditingRequest.builder()
                .photoUrl("https://dzen.ru/")
                .build();

        when(reportRepository.findByIdAndUserId(reportId, userId)).thenReturn(Optional.of(report));
        when(habitPhotoAllowedCacheRepository.existsById(habitId)).thenReturn(false);

        assertThatThrownBy(() -> underTest.changeReportPhoto(reportId, request, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .hasMessageContaining("Эта привычка не подразумевает фотоотчёты");
    }

    // TODO: тест на changeReportPhoto когда некорректный URL фото

    @Test
    void canDeleteReport() {
        String userIdStr = "3";
        Long userId = 3L;
        Long reportId = 2L;

        when(reportRepository.existsByIdAndUserId(reportId, userId)).thenReturn(true);

        underTest.deleteReport(reportId, userIdStr);

        verify(reportRepository).deleteById(reportId);
    }

    @Test
    void failDeleteReportWhenUserDoesNotHaveThatReport() {
        String userIdStr = "3";
        Long userId = 3L;
        Long reportId = 2L;

        when(reportRepository.existsByIdAndUserId(reportId, userId)).thenReturn(false);

        assertThatThrownBy(() -> underTest.deleteReport(reportId, userIdStr))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .hasMessageContaining("У текущего пользователя отсутствует указанный отчёт");

        verify(reportRepository, never()).deleteById(reportId);
    }

}