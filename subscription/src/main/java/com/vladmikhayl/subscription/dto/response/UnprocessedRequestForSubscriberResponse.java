package com.vladmikhayl.subscription.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Еще не обработанная заявка на подписку, отправленная конкретным пользователем")
public class UnprocessedRequestForSubscriberResponse {

    @Schema(description = "ID привычки, на которую еще не обработана заявка", example = "10")
    private Long habitId;

}
