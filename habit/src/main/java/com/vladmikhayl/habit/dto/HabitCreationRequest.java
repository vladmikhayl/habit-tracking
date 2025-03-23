package com.vladmikhayl.habit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vladmikhayl.habit.entity.FrequencyType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.DayOfWeek;
import java.util.Set;

@Data
public class HabitCreationRequest {

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @JsonProperty("isPhotoAllowed")
    private boolean isPhotoAllowed;

    @JsonProperty("isHarmful")
    private boolean isHarmful;

    @Min(value = 1, message = "Duration must be at least 1 day, if it is provided")
    @Max(value = 730, message = "Duration must not exceed 730 days")
    private Integer durationDays;

    @NotNull(message = "Frequency type must be specified")
    private FrequencyType frequencyType;

    private Set<DayOfWeek> daysOfWeek;

    @Min(value = 1, message = "Times per week must be from 1 to 7")
    @Max(value = 7, message = "Times per week must be from 1 to 7")
    private Integer timesPerWeek;

    @Min(value = 1, message = "Times per month must be from 1 to 31")
    @Max(value = 31, message = "Times per month must be from 1 to 31")
    private Integer timesPerMonth;

    @AssertTrue(message = "Invalid frequency settings")
    public boolean isValidFrequency() {
        if (frequencyType == null) {
            return false;
        }
        return switch (frequencyType) {
            case WEEKLY_ON_DAYS -> daysOfWeek != null && !daysOfWeek.isEmpty() && timesPerWeek == null && timesPerMonth == null;
            case WEEKLY_X_TIMES -> timesPerWeek != null && daysOfWeek == null && timesPerMonth == null;
            case MONTHLY_X_TIMES -> timesPerMonth != null && daysOfWeek == null && timesPerWeek == null;
        };
    }

    @AssertTrue(message = "A habit with this FrequencyType cannot be harmful")
    public boolean isValidHarmful() {
        return !isHarmful || frequencyType == FrequencyType.WEEKLY_ON_DAYS;
    }

}
