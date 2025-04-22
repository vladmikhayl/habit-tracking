package com.vladmikhayl.e2e.dto.habit;
import com.vladmikhayl.e2e.entity.FrequencyType;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.DayOfWeek;
import java.util.Set;

@Data
@AllArgsConstructor
public class HabitCreationRequest {

    private String name;

    private String description;

    @JsonProperty("isPhotoAllowed")
    private Boolean isPhotoAllowed;

    @JsonProperty("isHarmful")
    private Boolean isHarmful;

    private Integer durationDays;

    private FrequencyType frequencyType;

    private Set<DayOfWeek> daysOfWeek;

    private Integer timesPerWeek;

    private Integer timesPerMonth;
}
