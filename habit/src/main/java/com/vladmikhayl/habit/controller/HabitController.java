package com.vladmikhayl.habit.controller;

import com.vladmikhayl.habit.dto.HabitCreationRequest;
import com.vladmikhayl.habit.dto.HabitEditingRequest;
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

    @PutMapping("/{habitId}/edit")
    public ResponseEntity<Void> editHabit(
            @PathVariable Long habitId,
            @Valid @RequestBody HabitEditingRequest habitEditingRequest,
            @RequestHeader("X-User-Id") String userId
    ) {
        habitService.editHabit(habitId, habitEditingRequest, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/all-user-habits")
    public ResponseEntity<HabitsListResponse> getAllUserHabits(
            @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(habitService.getAllUserHabits(userId));
    }

}
