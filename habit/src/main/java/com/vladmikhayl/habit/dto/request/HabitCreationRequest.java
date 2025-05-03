package com.vladmikhayl.habit.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vladmikhayl.habit.entity.FrequencyType;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.util.Set;

@Data
@Builder
@Schema(description = "Запрос на создание привычки")
public class HabitCreationRequest {

    @NotBlank(message = "Не указано название")
    @Size(max = 255, message = "Название не может превышать 255 символов")
    @Schema(
            description = "Название",
            example = "Бегать по понедельникам",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @Size(max = 1000, message = "Описание не может превышать 1000 символов")
    @Schema(
            description = "Описание",
            example = "Бег это очень полезно, поэтому я решил по понедельникам делать пробежку в течение месяца"
    )
    private String description;

    @JsonProperty("isPhotoAllowed")
    @Schema(description = "Подразумевает ли эта привычка фотоотчёт", example = "false")
    private boolean isPhotoAllowed;

//    @JsonProperty("isHarmful")
//    @Schema(description = "Является ли эта привычка вредной", example = "false")
//    private boolean isHarmful;

    @Min(value = 1, message = "Если длительность указана, то она должна составлять хотя бы 1 день")
    @Max(value = 730, message = "Длительность не может превышать 730 дней")
    @Schema(description = "Длительность привычки в днях", example = "30")
    private Integer durationDays;

    @NotNull(message = "Не указана частота выполнения")
    @Schema(
            description = "Частота выполнения привычки",
            example = "WEEKLY_ON_DAYS",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private FrequencyType frequencyType;

    @Schema(description = "В какие дни недели привычка должна выполняться (только для WEEKLY_ON_DAYS, иначе null)")
    private Set<DayOfWeek> daysOfWeek;

    @Min(value = 1, message = "Число выполнений в неделю должно быть от 1 до 7")
    @Max(value = 7, message = "Число выполнений в неделю должно быть от 1 до 7")
    @Schema(description = "Сколько раз в неделю привычка должна выполняться (только для WEEKLY_X_TIMES, иначе null)", example = "null")
    private Integer timesPerWeek;

    @Min(value = 1, message = "Число выполнений в месяц должно быть от 1 до 31")
    @Max(value = 31, message = "Число выполнений в месяц должно быть от 1 до 31")
    @Schema(description = "Сколько раз в месяц привычка должна выполняться (только для MONTHLY_X_TIMES, иначе null)", example = "null")
    private Integer timesPerMonth;

    @AssertTrue(message = "Неверно указана частота выполнения")
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

//    @AssertTrue(message = "Привычка может быть вредной, только если она выполняется в определённые дни недели")
//    @Hidden
//    public boolean isValidHarmful() {
//        return !isHarmful || frequencyType == FrequencyType.WEEKLY_ON_DAYS;
//    }

}
