package com.vladmikhayl.commons.dto;

import lombok.Builder;

@Builder
public record HabitDeletedEvent(Long habitId) {}
