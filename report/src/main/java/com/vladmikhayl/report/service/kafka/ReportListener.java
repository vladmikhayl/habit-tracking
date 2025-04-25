package com.vladmikhayl.report.service.kafka;

import com.vladmikhayl.commons.dto.HabitCreatedEvent;
import com.vladmikhayl.commons.dto.HabitDeletedEvent;
import com.vladmikhayl.report.entity.HabitPhotoAllowedCache;
import com.vladmikhayl.report.repository.HabitPhotoAllowedCacheRepository;
import com.vladmikhayl.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportListener {

    private final HabitPhotoAllowedCacheRepository habitPhotoAllowedCacheRepository;

    private final ReportRepository reportRepository;

    @KafkaListener(topics = "habit-created", groupId = "report-group")
    public void listen(HabitCreatedEvent event) {
        log.info("Получено событие: создана привычка {}", event.habitId());

        if (event.isPhotoAllowed()) {
            habitPhotoAllowedCacheRepository.save(
                    HabitPhotoAllowedCache.builder()
                            .habitId(event.habitId())
                            .build()
            );
            log.info("В таблицу habits_photo_allowed_cache добавлена привычка {}", event.habitId());
        }
    }

    @Transactional
    @KafkaListener(topics = "habit-deleted", groupId = "report-group")
    public void listen(HabitDeletedEvent event) {
        Long habitId = event.habitId();

        log.info("Получено событие: удалена привычка {}", habitId);

        habitPhotoAllowedCacheRepository.deleteById(habitId);

        log.info("Из таблицы habits_photo_allowed_cache удалена привычка {} (если она там существовала)", habitId);

        reportRepository.deleteByHabitId(habitId);

        log.info("Удалены все отчеты о привычке {} (если они существовали)", habitId);
    }

}
