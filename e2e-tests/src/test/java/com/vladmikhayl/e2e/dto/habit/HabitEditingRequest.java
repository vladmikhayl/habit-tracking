package com.vladmikhayl.e2e.dto.habit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
public class HabitEditingRequest {

    private String description;

    @JsonProperty("isHarmful")
    private Boolean isHarmful;

    private Integer durationDays;

}
