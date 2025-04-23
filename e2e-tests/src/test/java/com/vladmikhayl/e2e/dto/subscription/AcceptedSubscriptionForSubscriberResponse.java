package com.vladmikhayl.e2e.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AcceptedSubscriptionForSubscriberResponse {

    private Long habitId;

    private String habitName;

}
