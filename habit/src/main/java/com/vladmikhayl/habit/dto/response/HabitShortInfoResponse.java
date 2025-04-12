package com.vladmikhayl.habit.dto.response;

import com.vladmikhayl.habit.entity.FrequencyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Короткая информация о конкретной привычке за конкретный день")
public class HabitShortInfoResponse {

    @Schema(description = "Название", example = "Бегать по понедельникам")
    private String name;

    @Schema(description = "Отмечена ли привычка выполненной в этот день", example = "true")
    private Boolean isCompleted;

    @Schema(description = "Количество подписчиков на привычку", example = "2")
    private Integer subscribersCount;

    @Schema(description = "Частота выполнения привычки", example = "WEEKLY_X_TIMES")
    private FrequencyType frequencyType;

    // Только для привычек с WEEKLY X TIMES или MONTHLY X TIMES (иначе null)
    @Schema(
            description = "Сколько раз привычка была выполнена за период (неделю/месяц), к которому относится выбранный день " +
                    "(только для WEEKLY X TIMES или MONTHLY X TIMES, иначе null)",
            example = "3"
    )
    private Integer completionsInPeriod;

    // Только для привычек с WEEKLY X TIMES или MONTHLY X TIMES (иначе null)
    @Schema(
            description = "Сколько выполнений в неделю/месяц предусматривает привычка " +
                    "(только для WEEKLY X TIMES или MONTHLY X TIMES, иначе null)",
            example = "5"
    )
    private Integer completionsPlannedInPeriod;

}
