package com.vladmikhayl.habit.controller;

import com.vladmikhayl.habit.dto.HabitCreationRequest;
import com.vladmikhayl.habit.dto.HabitsListResponse;
import com.vladmikhayl.habit.service.HabitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    @PostMapping("/create")
    public ResponseEntity<Void> createHabit(
            @Valid @RequestBody HabitCreationRequest habitCreationRequest,
            @RequestHeader("X-User-Id") String userId
    ) {
        habitService.createHabit(habitCreationRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/all-user-habits")
    public ResponseEntity<HabitsListResponse> getAllUserHabits(
            @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(habitService.getAllUserHabits(userId));
    }

}
