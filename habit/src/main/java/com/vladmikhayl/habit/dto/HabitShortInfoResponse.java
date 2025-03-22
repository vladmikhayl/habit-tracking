package com.vladmikhayl.habit.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HabitShortInfoResponse {

    private String name;

    private int subsAmount;

    private boolean isCompletedNow;

}
