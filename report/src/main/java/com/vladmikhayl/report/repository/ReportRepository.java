package com.vladmikhayl.report.repository;

import com.vladmikhayl.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByHabitIdAndDate(Long habitId, LocalDate date);

}
