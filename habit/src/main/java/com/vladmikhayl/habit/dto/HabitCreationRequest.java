package com.vladmikhayl.habit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HabitCreationRequest {

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Frequency cannot be blank")
    @Size(max = 255, message = "Frequency must not exceed 255 characters")
    private String frequency; // TODO: потом наверно поменяется

    private boolean photoAllowed;

    @Min(value = 1, message = "Duration must be at least 1 day, if it is provided")
    @Max(value = 730, message = "Duration must not exceed 730 days")
    private Integer durationDays;

}
