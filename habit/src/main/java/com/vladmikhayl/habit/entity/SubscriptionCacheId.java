package com.vladmikhayl.habit.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionCacheId implements Serializable {

    private Long habitId;

    private Long subscriberId;

}
