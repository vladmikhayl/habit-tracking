package com.vladmikhayl.report.service;

import com.vladmikhayl.report.dto.request.ReportCreationRequest;
import com.vladmikhayl.report.dto.request.ReportPhotoEditingRequest;
import com.vladmikhayl.report.entity.Report;
import com.vladmikhayl.report.repository.HabitPhotoAllowedCacheRepository;
import com.vladmikhayl.report.repository.ReportRepository;
import com.vladmikhayl.report.service.feign.HabitClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    @Value("${internal.token}")
    private String internalToken;

    private final ReportRepository reportRepository;

    private final HabitPhotoAllowedCacheRepository habitPhotoAllowedCacheRepository;

    private final HabitClient habitClient;

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Неверный формат ID пользователя");
        }
    }

    // TODO: проверять что URL фото правильное
    public void createReport(
            ReportCreationRequest request,
            String userId
    ) {
        Long userIdLong = parseUserId(userId);

        boolean isHabitCurrentAtThatDateForThatUser = getIsCurrentOrThrow(request.getHabitId(), userIdLong, request.getDate());

        if (!isHabitCurrentAtThatDateForThatUser) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Эта привычка не является текущей в указанный день для текущего пользователя");
        }

        Long habitId = request.getHabitId();

        if (reportRepository.existsByHabitIdAndDate(habitId, request.getDate())) {
            throw new DataIntegrityViolationException("Эта привычка уже отмечена как выполненная в указанный день");
        }

        if (request.getPhotoUrl() != null && !habitPhotoAllowedCacheRepository.existsById(habitId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "К отчёту было прикреплено фото, хотя эта привычка не подразумевает фотоотчёты");
        }

        if (request.getDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Этот день ещё не наступил");
        }

        Report report = Report.builder()
                .userId(userIdLong)
                .habitId(habitId)
                .date(request.getDate())
                .photoUrl(request.getPhotoUrl())
                .build();

        reportRepository.save(report);
    }

    // TODO: проверять что URL фото правильное
    @Transactional
    public void changeReportPhoto(
            Long reportId,
            ReportPhotoEditingRequest request,
            String userId
    ) {
        Long userIdLong = parseUserId(userId);

        Report report = reportRepository.findByIdAndUserId(reportId, userIdLong)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "У текущего пользователя отсутствует указанный отчёт"));

        long habitId = report.getHabitId();

        if (!habitPhotoAllowedCacheRepository.existsById(habitId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Эта привычка не подразумевает фотоотчёты");
        }

        String photoUrl = request.getPhotoUrl();

        if (photoUrl == null) {
            return;
        }

        // Чтобы положить значение null в поле photoUrl, в качестве этого параметра нужно передать пустую строку
        if (photoUrl.isEmpty()) {
            report.setPhotoUrl(null);
        } else {
            report.setPhotoUrl(photoUrl);
        }
    }

    public void deleteReport(
            Long reportId,
            String userId
    ) {
        Long userIdLong = parseUserId(userId);

        if (!reportRepository.existsByIdAndUserId(reportId, userIdLong)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "У текущего пользователя отсутствует указанный отчёт");
        }

        reportRepository.deleteById(reportId);
    }

    private boolean getIsCurrentOrThrow(Long habitId, Long userId, LocalDate date) {
        try {
            return habitClient.isCurrent(internalToken, habitId, userId, date).getBody();
        } catch (FeignException.ServiceUnavailable e) {
            log.error("Микросервис Habit недоступен");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Произошла внутренняя ошибка");
        } catch (FeignException e) {
            log.error("Микросервис Habit вернул ошибку");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Произошла внутренняя ошибка");
        }
    }

}
