package com.vladmikhayl.habit.dto.response;

import com.vladmikhayl.habit.entity.FrequencyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@Schema(description = "Подробная информация об общем состоянии конкретной привычки")
public class HabitGeneralInfoResponse {

    @Schema(description = "Уникальный ID", example = "17")
    private Long id;

    @Schema(description = "Название", example = "Бегать по понедельникам")
    private String name;

    @Schema(
            description = "Описание",
            example = "Бег это очень полезно, поэтому я решил по понедельникам делать пробежку в течение месяца"
    )
    private String description;

    @Schema(description = "Подразумевает ли эта привычка фотоотчёт", example = "false")
    private Boolean isPhotoAllowed;

    @Schema(description = "Является ли эта привычка вредной", example = "false")
    private Boolean isHarmful;

    @Schema(description = "Длительность привычки в днях", example = "30")
    private Integer durationDays;

    @Schema(
            description = "Сколько дней осталось до окончания привычки (только для привычек, у которых определена длительность, иначе null)",
            example = "28"
    )
    private Integer howManyDaysLeft;

    @Schema(description = "Частота выполнения привычки", example = "WEEKLY_ON_DAYS")
    private FrequencyType frequencyType;

    @Schema(description = "В какие дни недели привычка должна выполняться (только для WEEKLY_ON_DAYS, иначе null)")
    private Set<DayOfWeek> daysOfWeek;

    @Schema(description = "Сколько раз в неделю привычка должна выполняться (только для WEEKLY_X_TIMES, иначе null)", example = "null")
    private Integer timesPerWeek;

    @Schema(description = "Сколько раз в месяц привычка должна выполняться (только для MONTHLY_X_TIMES, иначе null)", example = "null")
    private Integer timesPerMonth;

    @Schema(description = "Когда привычка была создана")
    private LocalDateTime createdAt;

    @Schema(description = "Количество подписчиков на привычку", example = "2")
    private Integer subscribersCount;

}
