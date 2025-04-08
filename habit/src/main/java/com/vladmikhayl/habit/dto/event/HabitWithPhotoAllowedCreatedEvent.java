package com.vladmikhayl.habit.dto.event;

import lombok.Builder;

@Builder
public record HabitWithPhotoAllowedCreatedEvent(Long habitId) {}
