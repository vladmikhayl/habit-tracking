package com.vladmikhayl.habit.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HabitsListResponse {

    private List<HabitShortInfoResponse> habits;

}
