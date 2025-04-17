package com.vladmikhayl.subscription.service.kafka;

import com.vladmikhayl.subscription.dto.event.AcceptedSubscriptionCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendAcceptedSubscriptionCreatedEvent(AcceptedSubscriptionCreatedEvent event) {
        kafkaTemplate.send("accepted-subscription-created", event);
    }

}
