package com.vladmikhayl.subscription.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "habits_cache")
public class HabitCache {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "habits_cache_seq")
    @SequenceGenerator(name = "habits_cache_seq", sequenceName = "habits_cache_seq", allocationSize = 1)
    private Long id;

    private Long habitId;

    private Long creatorId;

    private String habitName;

}
