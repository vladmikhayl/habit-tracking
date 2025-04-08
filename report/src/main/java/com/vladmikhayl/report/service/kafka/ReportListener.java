package com.vladmikhayl.report.service.kafka;

import com.vladmikhayl.habit.dto.event.HabitCreatedEvent;
import com.vladmikhayl.report.entity.HabitPhotoAllowedCache;
import com.vladmikhayl.report.repository.HabitPhotoAllowedCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportListener {

    private final HabitPhotoAllowedCacheRepository habitPhotoAllowedCacheRepository;

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

}
