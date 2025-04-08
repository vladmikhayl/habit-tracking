package com.vladmikhayl.report.service.kafka;

import com.vladmikhayl.habit.dto.event.HabitWithPhotoAllowedCreatedEvent;
import com.vladmikhayl.report.entity.HabitPhotoAllowedCache;
import com.vladmikhayl.report.repository.HabitPhotoAllowedCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportListener {

    private final HabitPhotoAllowedCacheRepository habitPhotoAllowedCacheRepository;

    @KafkaListener(topics = "habit-with-photo-allowed-created", groupId = "report-group")
    public void listen(HabitWithPhotoAllowedCreatedEvent event) {
        habitPhotoAllowedCacheRepository.save(
                HabitPhotoAllowedCache.builder()
                        .habitId(event.habitId())
                        .build()
        );
        System.out.println("Получено событие: создана привычка " + event.habitId() + ", предусматривающая фотоотчет");
    }

}
