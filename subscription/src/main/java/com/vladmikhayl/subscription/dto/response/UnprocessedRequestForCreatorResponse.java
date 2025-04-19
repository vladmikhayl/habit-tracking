package com.vladmikhayl.subscription.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Еще не обработанная заявка на подписку, отправленная на конкретную привычку")
public class UnprocessedRequestForCreatorResponse {

    @Schema(description = "ID подписки", example = "45")
    private Long subscriptionId;

    @Schema(description = "Логин пользователя, который отправил эту заявку", example = "user10")
    private String subscriberLogin;

}
