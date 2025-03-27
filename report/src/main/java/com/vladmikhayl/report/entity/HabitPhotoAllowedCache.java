package com.vladmikhayl.report.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "habits_photo_allowed_cache")
public class HabitPhotoAllowedCache {

    @Id
    private Long habitId;

}
