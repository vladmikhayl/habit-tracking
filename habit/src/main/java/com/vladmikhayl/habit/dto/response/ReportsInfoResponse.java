package com.vladmikhayl.habit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@Schema(description = "Подробная информация о конкретной привычке, связанная с ее выполнением и отчетами о ней")
public class ReportsInfoResponse {

    // Для всех привычек
    @Schema(description = "Сколько раз была выполнена привычка за все время", example = "17")
    private int completionsInTotal;

    // Только для привычек с WEEKLY ON DAYS (иначе null)
    // Если еще не начался ни один день, в который нужно выполнить привычку, то сюда кладется null
    @Schema(
            description = "Сколько раз привычка успешно выполнена в процентах от всех запланированных выполнений " +
                    "(только для WEEKLY ON DAYS, иначе null)",
            example = "90"
    )
    private Integer completionsPercent;

    // Только для привычек с WEEKLY ON DAYS (иначе null)
    // Если еще не завершился ни один день, в который нужно выполнить привычку, и при этом привычка не была выполнена
    // в текущий день, то сюда кладется null
    @Schema(
            description = "Сколько дней в текущей серии выполнения привычки, то есть сколько последних дней подряд " +
                    "успешно выполнена эта привычка (только для WEEKLY ON DAYS, иначе null)",
            example = "8"
    )
    private Integer serialDays;

    // Только для привычек с WEEKLY X TIMES или MONTHLY X TIMES (иначе null)
    @Schema(
            description = "Сколько раз привычка была выполнена за текущую неделю/месяц " +
                    "(только для WEEKLY X TIMES или MONTHLY X TIMES, иначе null)",
            example = "null"
    )
    private Integer completionsInPeriod;

    // Только для привычек с WEEKLY X TIMES или MONTHLY X TIMES (иначе null)
    @Schema(
            description = "Сколько выполнений в неделю/месяц предусматривает привычка " +
                    "(только для WEEKLY X TIMES или MONTHLY X TIMES, иначе null)",
            example = "null"
    )
    private Integer completionsPlannedInPeriod;

    // Для всех привычек
    @Schema(description = "Список дней, в которые привычка выполнена")
    private List<LocalDate> completedDays;

    // Только для привычек с WEEKLY ON DAYS (иначе null)
    @Schema(
            description = "Список дней, в которые привычка не выполнена, хотя должна была быть выполнена " +
                    "(только для WEEKLY ON DAYS, иначе null)"
    )
    private List<LocalDate> uncompletedDays;

}
