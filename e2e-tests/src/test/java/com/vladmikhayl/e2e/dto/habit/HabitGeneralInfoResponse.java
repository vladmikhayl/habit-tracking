package com.vladmikhayl.e2e.dto.habit;
import com.vladmikhayl.e2e.entity.FrequencyType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HabitGeneralInfoResponse {

    private Long id;

    private String name;

    private String description;

    private Boolean isPhotoAllowed;

    private Boolean isHarmful;

    private Integer durationDays;

    private Integer howManyDaysLeft;

    private FrequencyType frequencyType;

    private Set<DayOfWeek> daysOfWeek;

    private Integer timesPerWeek;

    private Integer timesPerMonth;

    private LocalDateTime createdAt;

    private Integer subscribersCount;

}
