package com.vladmikhayl.subscription.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Принятый подписчик на конкретную привычку")
public class AcceptedSubscriptionForCreatorResponse {

    @Schema(description = "Логин подписчика", example = "user10")
    private String subscriberLogin;

}
