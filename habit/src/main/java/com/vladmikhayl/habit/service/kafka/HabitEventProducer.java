package com.vladmikhayl.habit.service.kafka;

import com.vladmikhayl.commons.dto.HabitCreatedEvent;
import com.vladmikhayl.commons.dto.HabitDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HabitEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendHabitCreatedEvent(HabitCreatedEvent event) {
        kafkaTemplate.send("habit-created", event);
    }

    public void sendHabitDeletedEvent(HabitDeletedEvent event) {
        kafkaTemplate.send("habit-deleted", event);
    }

}
