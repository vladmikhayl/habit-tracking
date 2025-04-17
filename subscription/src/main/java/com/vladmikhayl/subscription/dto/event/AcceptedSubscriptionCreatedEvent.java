package com.vladmikhayl.subscription.dto.event;

import lombok.Builder;

@Builder
public record AcceptedSubscriptionCreatedEvent(Long habitId, Long subscriberId, String habitCreatorLogin) {}
