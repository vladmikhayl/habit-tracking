package com.vladmikhayl.subscription.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Список еще не обработанных заявок на подписку, которые отправил конкретный пользователь")
public class UnprocessedRequestsForSubscriberResponse {

    @Schema(description = "ID привычек, на которые еще не обработана заявка")
    private List<Long> habitIds;

}
