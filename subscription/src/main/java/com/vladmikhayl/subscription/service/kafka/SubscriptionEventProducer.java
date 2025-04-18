package com.vladmikhayl.subscription.service.kafka;

import com.vladmikhayl.commons.dto.AcceptedSubscriptionCreatedEvent;
import com.vladmikhayl.commons.dto.AcceptedSubscriptionDeletedEvent;
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

    public void sendAcceptedSubscriptionDeletedEvent(AcceptedSubscriptionDeletedEvent event) {
        kafkaTemplate.send("accepted-subscription-deleted", event);
    }

}
