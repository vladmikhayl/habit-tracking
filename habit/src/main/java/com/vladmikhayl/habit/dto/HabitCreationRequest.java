package com.vladmikhayl.habit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vladmikhayl.habit.entity.FrequencyType;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.util.Set;

@Data
@Builder
@Schema(description = "Запрос на создание привычки")
public class HabitCreationRequest {

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Schema(
            description = "Название",
            example = "Бегать по понедельникам",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(
            description = "Описание",
            example = "Бег это очень полезно, поэтому я решил по понедельникам делать пробежку в течение месяца"
    )
    private String description;

    @JsonProperty("isPhotoAllowed")
    @Schema(description = "Подразумевает ли эта привычка фотоотчёт", example = "false")
    private boolean isPhotoAllowed;

    @JsonProperty("isHarmful")
    @Schema(description = "Является ли эта привычка вредной", example = "false")
    private boolean isHarmful;

    @Min(value = 1, message = "Duration must be at least 1 day, if it is provided")
    @Max(value = 730, message = "Duration must not exceed 730 days")
    @Schema(description = "Длительность привычки в днях", example = "30")
    private Integer durationDays;

    @NotNull(message = "Frequency type must be specified")
    @Schema(
            description = "Частота выполнения привычки",
            example = "WEEKLY_ON_DAYS",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private FrequencyType frequencyType;

    @Schema(description = "В какие дни недели привычка должна выполняться (только для WEEKLY_ON_DAYS, иначе null)")
    private Set<DayOfWeek> daysOfWeek;

    @Min(value = 1, message = "Times per week must be from 1 to 7")
    @Max(value = 7, message = "Times per week must be from 1 to 7")
    @Schema(description = "Сколько раз в неделю привычка должна выполняться (только для WEEKLY_X_TIMES, иначе null)", example = "null")
    private Integer timesPerWeek;

    @Min(value = 1, message = "Times per month must be from 1 to 31")
    @Max(value = 31, message = "Times per month must be from 1 to 31")
    @Schema(description = "Сколько раз в месяц привычка должна выполняться (только для MONTHLY_X_TIMES, иначе null)", example = "null")
    private Integer timesPerMonth;

    @AssertTrue(message = "Invalid frequency settings")
    @Hidden
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
    @Hidden
    public boolean isValidHarmful() {
        return !isHarmful || frequencyType == FrequencyType.WEEKLY_ON_DAYS;
    }

}
