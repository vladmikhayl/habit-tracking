package com.vladmikhayl.e2e.dto.habit;
import com.vladmikhayl.e2e.entity.FrequencyType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscribedHabitShortInfoResponse {

    private Long habitId;

    private String creatorLogin;

    private String name;

    private Boolean isCompleted;

    private Integer subscribersCount;

    private FrequencyType frequencyType;

    private Integer completionsInPeriod;

    private Integer completionsPlannedInPeriod;

}
