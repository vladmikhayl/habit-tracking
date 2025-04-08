package com.vladmikhayl.habit.service.kafka;

import com.vladmikhayl.habit.dto.event.HabitWithPhotoAllowedCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HabitEventProducer {

    private final KafkaTemplate<String, HabitWithPhotoAllowedCreatedEvent> kafkaTemplate;

    public void sendHabitWithPhotoAllowedCreatedEvent(HabitWithPhotoAllowedCreatedEvent event) {
        kafkaTemplate.send("habit-with-photo-allowed-created", event);
    }

}
