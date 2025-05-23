package com.vladmikhayl.report.repository;

import com.vladmikhayl.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByHabitIdAndDate(Long habitId, LocalDate date);

    boolean existsByIdAndUserId(Long id, Long userId);

    Optional<Report> findByHabitIdAndDate(Long habitId, LocalDate date);

    Optional<Report> findByIdAndUserId(Long id, Long userId);

    List<Report> findAllByHabitId(Long habitId);

    int countByHabitIdAndDateBetween(Long habitId, LocalDate startDate, LocalDate endDate);

    int countByHabitId(Long habitId);

    void deleteByHabitId(Long habitId);

}
