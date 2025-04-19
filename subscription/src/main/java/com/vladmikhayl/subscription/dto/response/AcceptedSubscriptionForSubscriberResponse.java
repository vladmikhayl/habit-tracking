package com.vladmikhayl.subscription.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Принятая подписка конкретного пользователя")
public class AcceptedSubscriptionForSubscriberResponse {

    @Schema(description = "ID привычки, на которую принята подписка", example = "10")
    private Long habitId;

    @Schema(description = "Название этой привычки", example = "Бегать по утрам")
    private String habitName;

}
