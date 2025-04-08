package com.vladmikhayl.habit.service.kafka;

import com.vladmikhayl.habit.dto.event.HabitCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HabitEventProducer {

    private final KafkaTemplate<String, HabitCreatedEvent> kafkaTemplate;

    public void sendHabitCreatedEvent(HabitCreatedEvent event) {
        kafkaTemplate.send("habit-created", event);
    }

}
