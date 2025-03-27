package com.vladmikhayl.habit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HabitEditingRequest {

    /*
    В этом классе если какое-то поле не было передано вообще (или там лежит null), то менять в БД его не нужно
    (то есть просто его в этом запросе не хотят редактировать)
    Чтобы положить значение null в поле durationDays, в качестве этого параметра нужно передать 0
     */

    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @JsonProperty("isHarmful")
    private Boolean isHarmful;

    @Min(value = 0, message = "Duration must be at least 1 day, if it is provided")
    @Max(value = 730, message = "Duration must not exceed 730 days")
    private Integer durationDays;

    @AssertTrue(message = "Name cannot be blank")
    public boolean isNameNotBlank() {
        return name == null || !name.isEmpty();
    }

}
