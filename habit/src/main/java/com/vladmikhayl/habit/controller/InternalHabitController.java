package com.vladmikhayl.habit.controller;

import com.vladmikhayl.habit.service.InternalHabitService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/internal/habits")
@RequiredArgsConstructor
@Hidden
public class InternalHabitController {

    private final InternalHabitService internalHabitService;

    @GetMapping("/{habitId}/is-current")
    public ResponseEntity<Boolean> isCurrent(
            @PathVariable Long habitId,
            @RequestParam Long userId,
            @RequestParam LocalDate date
    ) {
        boolean isCurrent = internalHabitService.isCurrent(habitId, userId, date);
        return ResponseEntity.ok(isCurrent);
    }

}
