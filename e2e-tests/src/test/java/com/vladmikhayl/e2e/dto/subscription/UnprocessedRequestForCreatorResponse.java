package com.vladmikhayl.e2e.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnprocessedRequestForCreatorResponse {

    private Long subscriptionId;

    private String subscriberLogin;

}
