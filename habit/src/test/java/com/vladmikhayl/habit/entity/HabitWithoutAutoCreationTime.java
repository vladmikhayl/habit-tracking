package com.vladmikhayl.habit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "habits")
// Это сущность, которая при интеграционных тестах сохраняется в ту же таблицу habits, но ее дату создания можно указывать вручную
public class HabitWithoutAutoCreationTime {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "habit_seq")
    @SequenceGenerator(name = "habit_seq", sequenceName = "habit_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean isPhotoAllowed = false;

    @Column(nullable = false)
    private boolean isHarmful = false;

    private Integer durationDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FrequencyType frequencyType;

    @ElementCollection
    @CollectionTable(name = "habit_week_days", joinColumns = @JoinColumn(name = "habit_id"))
    private Set<DayOfWeek> daysOfWeek; // если frequencyType == WEEKLY_ON_DAYS

    private Integer timesPerWeek; // если frequencyType == WEEKLY_X_TIMES

    private Integer timesPerMonth; // если frequencyType == MONTHLY_X_TIMES

    @Column(nullable = false)
    private LocalDateTime createdAt;

}
