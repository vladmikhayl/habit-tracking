package com.vladmikhayl.habit.controller;

import com.vladmikhayl.habit.entity.Habit;
import com.vladmikhayl.habit.service.HabitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    @GetMapping("/all-user-habits")
    public ResponseEntity<?> getAllUserHabits(
            @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(habitService.getAllUserHabits(userId));
    }

}
