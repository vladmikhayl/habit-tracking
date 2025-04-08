package com.vladmikhayl.habit.dto.event;

import lombok.Builder;

@Builder
public record HabitCreatedEvent(Long habitId, Long userId, boolean isPhotoAllowed) {}
