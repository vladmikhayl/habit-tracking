package com.vladmikhayl.commons.dto;

import lombok.Builder;

@Builder
public record HabitCreatedEvent(Long habitId, Long userId, boolean isPhotoAllowed, String habitName) {}
