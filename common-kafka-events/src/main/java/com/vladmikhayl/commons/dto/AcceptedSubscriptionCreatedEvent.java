package com.vladmikhayl.commons.dto;

import lombok.Builder;

@Builder
public record AcceptedSubscriptionCreatedEvent(Long habitId, Long subscriberId, String habitCreatorLogin) {}
