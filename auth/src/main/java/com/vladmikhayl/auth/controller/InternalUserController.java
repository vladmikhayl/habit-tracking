package com.vladmikhayl.auth.controller;

import com.vladmikhayl.auth.service.InternalUserService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
@Hidden
public class InternalUserController {

    private final InternalUserService internalUserService;

    @GetMapping("/{userId}/get-login")
    public ResponseEntity<String> getUserLogin(
            @PathVariable Long userId
    ) {
        String userLogin = internalUserService.getUserLogin(userId);
        return ResponseEntity.ok(userLogin);
    }

}
