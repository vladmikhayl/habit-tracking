package com.vladmikhayl.e2e.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class ReportCreationRequest {

    private Long habitId;

    private LocalDate date;

    private String photoUrl;

}
