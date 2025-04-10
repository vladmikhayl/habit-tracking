package com.vladmikhayl.habit.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscriptions_cache")
public class SubscriptionCache {

    @EmbeddedId
    private SubscriptionCacheId id;

    private String creatorLogin;

}
