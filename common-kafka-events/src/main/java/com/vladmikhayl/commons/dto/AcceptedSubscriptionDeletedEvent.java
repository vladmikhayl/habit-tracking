package com.vladmikhayl.commons.dto;

import lombok.Builder;

@Builder
public record AcceptedSubscriptionDeletedEvent(Long habitId, Long subscriberId) {}
