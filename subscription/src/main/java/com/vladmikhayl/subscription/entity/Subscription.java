package com.vladmikhayl.subscription.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subscription_seq")
    @SequenceGenerator(name = "subscription_seq", sequenceName = "subscription_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private Long habitId;

    @Column(nullable = false)
    private Long subscriberId;

    @Column(nullable = false)
    private boolean isAccepted = false;

}
