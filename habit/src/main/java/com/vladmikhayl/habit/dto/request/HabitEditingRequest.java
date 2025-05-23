package com.vladmikhayl.habit.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Запрос на изменение привычки")
public class HabitEditingRequest {

    /*
    В этом классе если какое-то поле не было передано вообще (или там лежит null), то менять в БД его не нужно
    (то есть просто его в этом запросе не хотят редактировать)
    Чтобы положить значение null в поле durationDays, в качестве этого параметра нужно передать 0
     */

//    @Size(max = 255, message = "Name must not exceed 255 characters")
//    @Schema(description = "Новое название", example = "Бегать")
//    private String name;

    @Size(max = 1000, message = "Описание не может превышать 1000 символов")
    @Schema(description = "Новое описание", example = "Бег это очень полезно, поэтому я решил делать пробежку в течение 2ух месяцев")
    private String description;

//    @JsonProperty("isHarmful")
//    @Schema(description = "Является ли теперь эта привычка вредной", example = "false")
//    private Boolean isHarmful;

    @Min(value = 0, message = "Если длительность указана, то она должна составлять хотя бы 1 день")
    @Max(value = 730, message = "Длительность не может превышать 730 дней")
    @Schema(
            description = "Новая длительность привычки в днях (чтобы указать, что привычка теперь вообще не должна " +
                    "иметь фиксированную длительность, нужно указать 0)",
            example = "60"
    )
    private Integer durationDays;

//    @AssertTrue(message = "Name cannot be blank")
//    @Hidden
//    public boolean isNameNotBlank() {
//        return name == null || !name.isEmpty();
//    }

}
