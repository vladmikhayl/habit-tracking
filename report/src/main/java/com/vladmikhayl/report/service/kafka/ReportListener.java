package com.vladmikhayl.report.service.kafka;

import com.vladmikhayl.commons.dto.HabitCreatedEvent;
import com.vladmikhayl.commons.dto.HabitDeletedEvent;
import com.vladmikhayl.report.entity.HabitPhotoAllowedCache;
import com.vladmikhayl.report.repository.HabitPhotoAllowedCacheRepository;
import com.vladmikhayl.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportListener {

    private final HabitPhotoAllowedCacheRepository habitPhotoAllowedCacheRepository;

    private final ReportRepository reportRepository;

    @KafkaListener(topics = "habit-created", groupId = "report-group")
    public void listen(HabitCreatedEvent event) {
        System.out.println("Получено событие: создана привычка " + event.habitId());

        if (event.isPhotoAllowed()) {
            habitPhotoAllowedCacheRepository.save(
                    HabitPhotoAllowedCache.builder()
                            .habitId(event.habitId())
                            .build()
            );
            System.out.println("В таблицу habits_photo_allowed_cache добавлена привычка " + event.habitId());
        }
    }

    @Transactional
    @KafkaListener(topics = "habit-deleted", groupId = "report-group")
    public void listen(HabitDeletedEvent event) {
        Long habitId = event.habitId();

        System.out.println("Получено событие: удалена привычка " + habitId);

        habitPhotoAllowedCacheRepository.deleteById(habitId);

        System.out.println("Из таблицы habits_photo_allowed_cache удалена привычка " + habitId + " (если она там существовала)");

        reportRepository.deleteByHabitId(habitId);

        System.out.println("Удалены все отчеты о привычке " + habitId + " (если они там существовали)");
    }

}
